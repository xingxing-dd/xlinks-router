package site.xlinks.ai.router.client.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.config.SmsProperties;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

/**
 * Verify code service with Redis storage + throttling.
 *
 * Key design:
 * - code: vcode:{scene}:{type}:{target}
 * - interval lock: vcode:interval:{scene}:{type}:{target}
 * - daily counter: vcode:daily:{yyyyMMdd}:{scene}:{type}:{target}
 */
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate redisTemplate;
    private final SmsProperties smsProperties;

    public VerifyCodeIssueResult issueCode(String scene, String codeType, String target) {
        String normalizedType = codeType == null ? null : codeType.trim().toLowerCase(Locale.ROOT);
        String normalizedTarget = target == null ? null : target.trim();

        if (scene == null || scene.isBlank() || normalizedType == null || normalizedType.isBlank() || normalizedTarget == null
                || normalizedTarget.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "scene/codeType/target 不能为空");
        }

        SmsProperties.VerifyCode cfg = smsProperties.getVerifyCode();
        int expireSeconds = Math.max(60, cfg.getExpireSeconds());
        int minIntervalSeconds = Math.max(0, cfg.getMinIntervalSeconds());
        int dailyLimit = Math.max(1, cfg.getDailyLimit());

        String intervalKey = intervalKey(scene, normalizedType, normalizedTarget);
        if (minIntervalSeconds > 0) {
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(intervalKey, "1", minIntervalSeconds, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(ok)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "发送过于频繁，请稍后再试");
            }
        }

        String dailyKey = dailyKey(LocalDate.now(), scene, normalizedType, normalizedTarget);
        Long daily = redisTemplate.opsForValue().increment(dailyKey);
        if (daily != null && daily == 1) {
            redisTemplate.expire(dailyKey, 2, TimeUnit.DAYS);
        }
        if (daily != null && daily > dailyLimit) {
            if (minIntervalSeconds > 0) {
                redisTemplate.delete(intervalKey);
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "今日验证码发送次数已达上限");
        }

        String code = generate6Digits();
        String codeKey = codeKey(scene, normalizedType, normalizedTarget);
        redisTemplate.opsForValue().set(codeKey, code, expireSeconds, TimeUnit.SECONDS);

        return new VerifyCodeIssueResult(codeKey, code, expireSeconds);
    }

    public void verifyOrThrow(String scene, String codeType, String target, String code) {
        String normalizedType = codeType == null ? null : codeType.trim().toLowerCase(Locale.ROOT);
        String normalizedTarget = target == null ? null : target.trim();

        if (scene == null || scene.isBlank() || normalizedType == null || normalizedType.isBlank() || normalizedTarget == null
                || normalizedTarget.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        String key = codeKey(scene, normalizedType, normalizedTarget);
        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null || !cached.equals(code.trim())) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }

        // One-time use
        redisTemplate.delete(key);
    }

    /**
     * 通过 token 获取验证码
     *
     * @param token 验证码存储的 Redis key
     * @return 验证码
     */
    public String getCodeByToken(String token) {
        return redisTemplate.opsForValue().get(token);
    }

    private static String generate6Digits() {
        int n = RANDOM.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private static String codeKey(String scene, String type, String target) {
        return "xlinks:" + scene + ":" + type + ":" + target;
    }

    private static String intervalKey(String scene, String type, String target) {
        return "xlinks:interval:" + scene + ":" + type + ":" + target;
    }

    private static String dailyKey(LocalDate day, String scene, String type, String target) {
        return "xlinks:daily:" + DAY_FMT.format(day) + ":" + scene + ":" + type + ":" + target;
    }

    public record VerifyCodeIssueResult(String token, String code, int expireSeconds) {}
}
