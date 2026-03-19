package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.auth.AuthLoginRequest;
import site.xlinks.ai.router.client.dto.auth.AuthLoginResponse;
import site.xlinks.ai.router.client.dto.auth.AuthRegisterRequest;
import site.xlinks.ai.router.client.dto.auth.RsaPublicKeyResponse;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendRequest;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.CustomerAccountService;
import site.xlinks.ai.router.common.result.Result;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerAccountService customerAccountService;

    @PostMapping("/rsa-public-key")
    public Result<RsaPublicKeyResponse> getRsaPublicKey() {
        RsaPublicKeyResponse response = new RsaPublicKeyResponse();
        response.setAlgorithm("RSA/ECB/PKCS1Padding");
        response.setPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmockPublicKeyForClientModule");
        return Result.success(response);
    }

    @PostMapping("/verify-code")
    public Result<VerifyCodeSendResponse> sendVerifyCode(@Valid @RequestBody VerifyCodeSendRequest request) {
        // TODO: 集成短信/邮箱服务发送验证码
        VerifyCodeSendResponse response = new VerifyCodeSendResponse();
        String message = "email".equalsIgnoreCase(request.getCodeType()) ? "邮箱验证码发送成功" : "短信验证码发送成功";
        response.setMessage(message);
        response.setMockCode("123456");
        response.setExpireSeconds(300);
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody AuthRegisterRequest request) {
        customerAccountService.register(request);
        return Result.success("注册成功", null);
    }

    @PostMapping("/login")
    public Result<AuthLoginResponse> login(@Valid @RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = customerAccountService.login(request.getUsername(), request.getPassword());
        return Result.success(response);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        customerAccountService.logout();
        return Result.success("登出成功", null);
    }
}
