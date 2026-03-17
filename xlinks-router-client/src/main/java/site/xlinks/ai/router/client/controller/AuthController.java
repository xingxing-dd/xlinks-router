package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.auth.AuthLoginRequest;
import site.xlinks.ai.router.client.dto.auth.AuthLoginResponse;
import site.xlinks.ai.router.client.dto.auth.AuthRegisterRequest;
import site.xlinks.ai.router.client.dto.auth.RsaPublicKeyResponse;
import site.xlinks.ai.router.client.dto.auth.SmsCodeSendRequest;
import site.xlinks.ai.router.client.dto.auth.SmsCodeSendResponse;
import site.xlinks.ai.router.common.result.Result;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/rsa-public-key")
    public Result<RsaPublicKeyResponse> getRsaPublicKey() {
        RsaPublicKeyResponse response = new RsaPublicKeyResponse();
        response.setAlgorithm("RSA/ECB/PKCS1Padding");
        response.setPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmockPublicKeyForClientModule");
        return Result.success(response);
    }

    @PostMapping("/sms-code")
    public Result<SmsCodeSendResponse> sendSmsCode(@Valid @RequestBody SmsCodeSendRequest request) {
        SmsCodeSendResponse response = new SmsCodeSendResponse();
        response.setMessage("验证码发送成功");
        response.setMockCode("123456");
        response.setExpireSeconds(300);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody AuthRegisterRequest request) {
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = new AuthLoginResponse();
        response.setAccessToken("mock-access-token-client-module");
        response.setExpiresIn(7200L);

        AuthLoginResponse.AuthUser user = new AuthLoginResponse.AuthUser();
        user.setId(1L);
        user.setEmail(request.getEmail());
        user.setStatus(1);
        response.setUser(user);
        return Result.success(response);
    }
}
