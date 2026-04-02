package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.client.dto.auth.AuthLoginResponse;
import site.xlinks.ai.router.client.dto.auth.AuthRegisterRequest;
import site.xlinks.ai.router.client.dto.auth.AuthResetPasswordRequest;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;

import java.util.Locale;

/**
 * 客户账号服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerAccountService {

    private static final String TARGET_TYPE_USERNAME = "username";
    private static final String TARGET_TYPE_PHONE = "phone";
    private static final String TARGET_TYPE_EMAIL = "email";
    private static final String SCENE_REGISTER = "register";
    private static final String SCENE_RESET_PASSWORD = "resetpwd";

    private final CustomerAccountMapper customerAccountMapper;
    private final PromotionService promotionService;
    private final TokenService tokenService;
    private final VerifyCodeService verifyCodeService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 注册新用户
     */
    @Transactional(rollbackFor = Exception.class)
    public void register(AuthRegisterRequest request) {
        String target = normalizeTarget(request.getTarget());
        String targetType = normalizeType(request.getTargetType());

        if (findByTarget(targetType, target) != null) {
            throwAlreadyExists(targetType);
        }

        verifyCodeService.verifyOrThrow(SCENE_REGISTER, targetType, target, request.getCode());

        CustomerAccount account = new CustomerAccount();
        if (TARGET_TYPE_PHONE.equals(targetType)) {
            account.setPhone(target);
        } else if (TARGET_TYPE_EMAIL.equals(targetType)) {
            account.setEmail(target);
        }
        account.setUsername(target);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setStatus(1);

        customerAccountMapper.insert(account);
        promotionService.bindInviterAndInitReward(account, request.getInviteCode());

        log.info("New customer account registered: {}", account.getId());
    }

    /**
     * 忘记密码后重置密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(AuthResetPasswordRequest request) {
        String target = normalizeTarget(request.getTarget());
        String targetType = normalizeType(request.getTargetType());
        CustomerAccount account = findByTarget(targetType, target);
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        verifyCodeService.verifyOrThrow(SCENE_RESET_PASSWORD, targetType, target, request.getCode());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        customerAccountMapper.updateById(account);

        log.info("Customer password reset completed: {}", account.getId());
    }

    /**
     * 验证发送验证码前的账号状态
     */
    public void validateVerifyCodeTarget(String scene, String codeType, String target) {
        String normalizedScene = normalizeType(scene);
        String normalizedCodeType = normalizeType(codeType);
        String normalizedTarget = normalizeTarget(target);

        if (SCENE_REGISTER.equals(normalizedScene)) {
            if (findByTarget(normalizedCodeType, normalizedTarget) != null) {
                throwAlreadyExists(normalizedCodeType);
            }
            return;
        }

        if (SCENE_RESET_PASSWORD.equals(normalizedScene)
                && findByTarget(normalizedCodeType, normalizedTarget) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
    }

    /**
     * 用户登录
     */
    public AuthLoginResponse login(String username, String password) {
        LambdaQueryWrapper<CustomerAccount> query = new LambdaQueryWrapper<>();
        query.eq(CustomerAccount::getUsername, username);
        CustomerAccount account = customerAccountMapper.selectOne(query);

        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (account.getStatus() != null && account.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        String token = tokenService.generateToken(account);

        AuthLoginResponse response = new AuthLoginResponse();
        response.setAccessToken(token);
        response.setExpiresIn(7200L);

        AuthLoginResponse.AuthUser user = new AuthLoginResponse.AuthUser();
        user.setId(account.getId());
        user.setEmail(account.getEmail());
        user.setStatus(account.getStatus());
        response.setUser(user);

        log.info("Customer account logged in: {}", account.getId());
        return response;
    }

    /**
     * 用户登出
     */
    public void logout() {
        var currentAccount = tokenService.getCurrentAccount();
        if (currentAccount != null) {
            tokenService.logout(currentAccount.getId());
        }
    }

    /**
     * 根据ID获取账户信息
     */
    public CustomerAccount getById(Long id) {
        return customerAccountMapper.selectById(id);
    }

    /**
     * 获取当前账户的邀请码，没有则自动生成。
     */
    public String getOrCreateInviteCode(Long accountId) {
        CustomerAccount account = getById(idOrThrow(accountId));
        promotionService.ensureInviteCode(account);
        return account.getInviteCode();
    }

    /**
     * 更新账户信息
     */
    public void updateAccount(CustomerAccount account) {
        customerAccountMapper.updateById(account);
    }

    private CustomerAccount findByTarget(String targetType, String target) {
        if (target == null || target.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标不能为空");
        }

        LambdaQueryWrapper<CustomerAccount> queryWrapper = new LambdaQueryWrapper<>();
        if (TARGET_TYPE_USERNAME.equals(targetType)) {
            queryWrapper.eq(CustomerAccount::getUsername, target);
        } else if (TARGET_TYPE_PHONE.equals(targetType)) {
            queryWrapper.eq(CustomerAccount::getPhone, target);
        } else if (TARGET_TYPE_EMAIL.equals(targetType)) {
            queryWrapper.eq(CustomerAccount::getEmail, target);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型");
        }
        return customerAccountMapper.selectOne(queryWrapper);
    }

    private void throwAlreadyExists(String targetType) {
        if (TARGET_TYPE_USERNAME.equals(targetType)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }
        if (TARGET_TYPE_PHONE.equals(targetType)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        if (TARGET_TYPE_EMAIL.equals(targetType)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型");
    }

    private String normalizeType(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeTarget(String value) {
        return value == null ? null : value.trim();
    }

    private Long idOrThrow(Long accountId) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return accountId;
    }
}
