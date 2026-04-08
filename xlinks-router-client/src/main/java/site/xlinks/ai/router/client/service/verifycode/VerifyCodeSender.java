package site.xlinks.ai.router.client.service.verifycode;

import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;

/**
 * Strategy interface for sending verification codes.
 */
public interface VerifyCodeSender {

    /**
     * Send verification code.
     *
     * @param scene business scene, such as register/resetpwd
     * @param target recipient target, such as phone or email
     * @param token verification-code token stored in Redis
     * @param expireSeconds expiration time in seconds
     * @return send result
     */
    VerifyCodeSendResponse send(String scene, String target, String token, int expireSeconds);

    /**
     * Supported code type.
     */
    String getSupportedCodeType();
}