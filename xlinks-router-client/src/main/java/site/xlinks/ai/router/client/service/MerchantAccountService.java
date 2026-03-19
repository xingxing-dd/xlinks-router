package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.dto.auth.AuthLoginResponse;
import site.xlinks.ai.router.client.dto.auth.AuthRegisterRequest;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.MerchantAccount;
import site.xlinks.ai.router.mapper.MerchantAccountMapper;

import java.util.regex.Pattern;

/**
 * 商户账户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAccountService {

    private final MerchantAccountMapper merchantAccountMapper;
    private final TokenService tokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 账号类型常量
    private static final String TARGET_TYPE_USERNAME = "username";
    private static final String TARGET_TYPE_PHONE = "phone";
    private static final String TARGET_TYPE_EMAIL = "email";

    /**
     * 注册新用户
     */
    public void register(AuthRegisterRequest request) {
        // 验证验证码（TODO: 实际实现验证码校验）
        if (request.getCode() == null || !request.getCode().equals("123456")) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        String target = request.getTarget();
        String targetType = request.getTargetType();

        // 检查账号是否已存在
        LambdaQueryWrapper<MerchantAccount> queryWrapper = new LambdaQueryWrapper<>();
        
        if (TARGET_TYPE_USERNAME.equalsIgnoreCase(targetType)) {
            queryWrapper.eq(MerchantAccount::getUsername, target);
        } else if (TARGET_TYPE_PHONE.equalsIgnoreCase(targetType)) {
            queryWrapper.eq(MerchantAccount::getPhone, target);
        } else if (TARGET_TYPE_EMAIL.equalsIgnoreCase(targetType)) {
            queryWrapper.eq(MerchantAccount::getEmail, target);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的账号类型");
        }

        Long count = merchantAccountMapper.selectCount(queryWrapper);
        if (count > 0) {
            if (TARGET_TYPE_USERNAME.equalsIgnoreCase(targetType)) {
                throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
            } else if (TARGET_TYPE_PHONE.equalsIgnoreCase(targetType)) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
            } else {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        // 创建新用户
        MerchantAccount account = new MerchantAccount();
        
        if (TARGET_TYPE_USERNAME.equalsIgnoreCase(targetType)) {
            account.setUsername(target);
        } else if (TARGET_TYPE_PHONE.equalsIgnoreCase(targetType)) {
            account.setPhone(target);
        } else if (TARGET_TYPE_EMAIL.equalsIgnoreCase(targetType)) {
            account.setEmail(target);
        }
        
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setStatus(1); // 启用状态

        merchantAccountMapper.insert(account);

        log.info("New merchant account registered: {}", account.getId());
    }

    /**
     * 用户登录
     */
    public AuthLoginResponse login(String username, String password) {
        // 用户名登录
        LambdaQueryWrapper<MerchantAccount> query = new LambdaQueryWrapper<>();
        query.eq(MerchantAccount::getUsername, username);
        MerchantAccount account = merchantAccountMapper.selectOne(query);

        // 验证用户是否存在
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证密码
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        // 检查账户状态
        if (account.getStatus() != null && account.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        // 生成Token
        String token = tokenService.generateToken(account);

        // 构建响应
        AuthLoginResponse response = new AuthLoginResponse();
        response.setAccessToken(token);
        response.setExpiresIn(7200L);

        AuthLoginResponse.AuthUser user = new AuthLoginResponse.AuthUser();
        user.setId(account.getId());
        user.setEmail(account.getEmail());
        user.setStatus(account.getStatus());
        response.setUser(user);

        log.info("Merchant account logged in: {}", account.getId());
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
    public MerchantAccount getById(Long id) {
        return merchantAccountMapper.selectById(id);
    }

    /**
     * 更新账户信息
     */
    public void updateAccount(MerchantAccount account) {
        merchantAccountMapper.updateById(account);
    }
}
