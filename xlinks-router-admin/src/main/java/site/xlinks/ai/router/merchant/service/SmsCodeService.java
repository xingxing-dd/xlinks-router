package site.xlinks.ai.router.merchant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.merchant.config.AuthProperties;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    public String sendCode(String email, String scene) {
        String cooldownKey = buildCooldownKey(email, scene);
        Boolean hasCooldown = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        // 此处后续替换成真正的邮件发送
        String code = generateCode();
        String codeKey = buildCodeKey(email, scene);
        stringRedisTemplate.opsForValue().set(codeKey, code, authProperties.getSmsCodeExpireSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", authProperties.getSmsSendIntervalSeconds(), TimeUnit.SECONDS);

        log.info("Send email code, email={}, scene={}, code={}", email, scene, code);
        return code;
    }

    public void verifyCode(String email, String scene, String code) {
        if (Boolean.TRUE.equals(authProperties.getSmsMockEnabled())) {
            log.warn("mock开启不验证邮箱验证码");
            return;
        }
        String codeKey = buildCodeKey(email, scene);
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        stringRedisTemplate.delete(codeKey);
    }

    private String buildCodeKey(String email, String scene) {
        return authProperties.getSmsCodePrefix() + scene + ":" + email;
    }

    private String buildCooldownKey(String email, String scene) {
        return buildCodeKey(email, scene) + ":cooldown";
    }

    private String generateCode() {
        int value = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}