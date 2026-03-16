package site.xlinks.ai.router.merchant.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.merchant.config.AuthProperties;
import site.xlinks.ai.router.merchant.dto.AuthTokenVO;
import site.xlinks.ai.router.merchant.dto.AuthUserVO;
import site.xlinks.ai.router.merchant.dto.UserLoginDTO;
import site.xlinks.ai.router.merchant.dto.UserRegisterDTO;
import site.xlinks.ai.router.merchant.entity.AuthLoginLog;
import site.xlinks.ai.router.merchant.entity.AuthUser;
import site.xlinks.ai.router.merchant.mapper.AuthUserMapper;

import java.time.LocalDateTime;

/**
 * 认证用户服务。
 */
@Service
@RequiredArgsConstructor
public class AuthUserService extends ServiceImpl<AuthUserMapper, AuthUser> {

    private final PasswordEncoder passwordEncoder;
    private final SmsCodeService smsCodeService;
    private final AuthSessionService authSessionService;
    private final AuthLoginLogService authLoginLogService;
    private final RsaCryptoService rsaCryptoService;
    private final AuthProperties authProperties;

    public void register(UserRegisterDTO dto, HttpServletRequest request) {
        assertUserNotExists(dto.getUsername(), dto.getMobile());
        smsCodeService.verifyCode(dto.getMobile(), "register", dto.getSmsCode());

        AuthUser user = new AuthUser();
        user.setUsername(dto.getUsername());
        String password = rsaCryptoService.decrypt(dto.getEncryptedPassword());
        validatePassword(password);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setMobile(dto.getMobile());
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(request));
        this.save(user);

        recordLog(user, "register", 1, null, request);
    }

    public AuthTokenVO login(UserLoginDTO dto, HttpServletRequest request) {
        AuthUser user = getByUsername(dto.getUsername());
        validateUserStatus(user);

        boolean useSms = StringUtils.hasText(dto.getSmsCode());
        if (useSms) {
            if (!StringUtils.hasText(dto.getMobile()) || !dto.getMobile().equals(user.getMobile())) {
                recordLog(user, "sms", 0, "手机号与账号不匹配", request);
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号与账号不匹配");
            }
            smsCodeService.verifyCode(dto.getMobile(), "login", dto.getSmsCode());
        } else {
            String password = rsaCryptoService.decrypt(dto.getEncryptedPassword());
            if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
                recordLog(user, "password", 0, "用户名或密码错误", request);
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
            }
            // 验证完密码后仍然要验证短信
            smsCodeService.verifyCode(dto.getMobile(), "login", dto.getSmsCode());
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(request));
        this.updateById(user);
        recordLog(user, useSms ? "sms" : "password", 1, null, request);
        return buildLoginResult(user);
    }

    public AuthUser getByUsername(String username) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getUsername, username);
        AuthUser user = this.getOne(wrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    public AuthUser getByMobile(String mobile) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getMobile, mobile);
        return this.getOne(wrapper);
    }

    private void assertUserNotExists(String username, String mobile) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getUsername, username).or().eq(AuthUser::getMobile, mobile);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "用户名或手机号已存在");
        }
    }

    private void validateUserStatus(AuthUser user) {
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 6 || password.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码长度需在 6-32 位之间");
        }
    }

    private AuthTokenVO buildLoginResult(AuthUser user) {
        String token = authSessionService.createToken(user);
        return AuthTokenVO.builder()
                .accessToken(token)
                .expiresIn(authProperties.getAuthTokenExpireSeconds())
                .user(AuthUserVO.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .mobile(user.getMobile())
                        .status(user.getStatus())
                        .build())
                .build();
    }

    private void recordLog(AuthUser user, String loginType, Integer status, String failureReason, HttpServletRequest request) {
        AuthLoginLog log = new AuthLoginLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setUsername(user.getUsername());
            log.setMobile(user.getMobile());
        }
        log.setLoginType(loginType);
        log.setLoginStatus(status);
        log.setLoginIp(getClientIp(request));
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setFailureReason(failureReason);
        authLoginLogService.record(log);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}