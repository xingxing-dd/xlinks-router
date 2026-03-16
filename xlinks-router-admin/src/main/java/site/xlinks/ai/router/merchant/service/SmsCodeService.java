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
 * 短信验证码服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final AuthProperties authProperties;

    public String sendCode(String mobile, String scene) {
        String cooldownKey = buildCooldownKey(mobile, scene);
        Boolean hasCooldown = stringRedisTemplate.hasKey(cooldownKey);
        if (Boolean.TRUE.equals(hasCooldown)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
        }

        //此处后续换成真正的发短信
        String code = generateCode();
        String codeKey = buildCodeKey(mobile, scene);
        stringRedisTemplate.opsForValue().set(codeKey, code, authProperties.getSmsCodeExpireSeconds(), TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(cooldownKey, "1", authProperties.getSmsSendIntervalSeconds(), TimeUnit.SECONDS);

        log.info("Send sms code, mobile={}, scene={}, code={}", mobile, scene, code);
        return code;
    }

    public void verifyCode(String mobile, String scene, String code) {
        if (Boolean.TRUE.equals(authProperties.getSmsMockEnabled())) {
            log.warn("mock开启不验证短信");
            return;
        }
        String codeKey = buildCodeKey(mobile, scene);
        String cachedCode = stringRedisTemplate.opsForValue().get(codeKey);
        if (cachedCode == null || !cachedCode.equals(code)) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID);
        }
        stringRedisTemplate.delete(codeKey);
    }

    private String buildCodeKey(String mobile, String scene) {
        return authProperties.getSmsCodePrefix() + scene + ":" + mobile;
    }

    private String buildCooldownKey(String mobile, String scene) {
        return buildCodeKey(mobile, scene) + ":cooldown";
    }

    private String generateCode() {
        int value = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}