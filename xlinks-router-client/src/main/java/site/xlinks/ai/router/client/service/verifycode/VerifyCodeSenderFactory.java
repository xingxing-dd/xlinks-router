package site.xlinks.ai.router.client.service.verifycode;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 验证码发送策略工厂
 * 使用 Spring 自动收集所有实现了 VerifyCodeSender 接口的策略
 */
@Slf4j
@Component
public class VerifyCodeSenderFactory {

    private final Map<String, VerifyCodeSender> senderMap;

    /**
     * 构造器注入所有 VerifyCodeSender 实现
     *
     * @param senders 所有实现了 VerifyCodeSender 接口的 Bean
     */
    public VerifyCodeSenderFactory(List<VerifyCodeSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toMap(
                        sender -> sender.getSupportedCodeType().toLowerCase(Locale.ROOT),
                        Function.identity()
                ));
        log.info("Registered verify code senders: {}", senderMap.keySet());
    }

     /**
      * 根据验证码类型获取对应的发送策略
      *
      * @param codeType 验证码类型 (如 "phone", "email")
      * @return 验证码发送策略
      * @throws BusinessException 如果没有找到对应的策略
      */
public VerifyCodeSender getSender(String codeType) {
        if (codeType == null || codeType.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码类型不能为空");
        }

        String normalizedType = codeType.trim().toLowerCase(Locale.ROOT);
        VerifyCodeSender sender = senderMap.get(normalizedType);

        if (sender == null) {
            log.warn("No verify code sender found for type: {}", codeType);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的验证码类型: " + codeType);
        }

        return sender;
    }

    /**
     * 检查是否支持指定的验证码类型
     *
     * @param codeType 验证码类型
     * @return 是否支持
     */
    public boolean isSupported(String codeType) {
        if (codeType == null || codeType.isBlank()) {
            return false;
        }
        return senderMap.containsKey(codeType.trim().toLowerCase(Locale.ROOT));
    }
}