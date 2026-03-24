package site.xlinks.ai.router.client.service.verifycode;

import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;

/**
 * 验证码发送策略接口
 * 使用策略模式实现不同类型验证码的发送，便于扩展
 */
public interface VerifyCodeSender {

    /**
     * 发送验证码
     *
     * @param target       目标地址（手机号或邮箱）
     * @param token        验证码 token（Redis key，用于后续验证）
     * @param expireSeconds 过期秒数
     * @return 发送结果响应
     */
    VerifyCodeSendResponse send(String target, String token, int expireSeconds);

    /**
     * 获取支持的验证码类型
     * 例如: "sms", "email"
     *
     * @return 支持的类型
     */
    String getSupportedCodeType();
}