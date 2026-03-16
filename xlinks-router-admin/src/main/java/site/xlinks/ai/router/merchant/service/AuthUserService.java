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
        assertUserNotExists(dto.getEmail());
        smsCodeService.verifyCode(dto.getEmail(), "register", dto.getEmailCode());

        AuthUser user = new AuthUser();
        String password = rsaCryptoService.decrypt(dto.getEncryptedPassword());
        validatePassword(password);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(request));
        this.save(user);

        recordLog(user, "register", 1, null, request);
    }

    public AuthTokenVO login(UserLoginDTO dto, HttpServletRequest request) {
        AuthUser user = getByEmail(dto.getEmail());
        validateUserStatus(user);

        String password = rsaCryptoService.decrypt(dto.getEncryptedPassword());
        if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, user.getPasswordHash())) {
            recordLog(user, "password", 0, "邮箱或密码错误", request);
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(request));
        this.updateById(user);
        recordLog(user, "password", 1, null, request);
        return buildLoginResult(user);
    }

    public AuthUser getByEmail(String email) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getEmail, email);
        AuthUser user = this.getOne(wrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void assertUserNotExists(String email) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getEmail, email);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已存在");
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
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .build())
                .build();
    }

    private void recordLog(AuthUser user, String loginType, Integer status, String failureReason, HttpServletRequest request) {
        AuthLoginLog log = new AuthLoginLog();
        if (user != null) {
            log.setUserId(user.getId());
            log.setEmail(user.getEmail());
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