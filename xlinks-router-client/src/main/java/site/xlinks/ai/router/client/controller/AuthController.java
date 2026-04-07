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
import site.xlinks.ai.router.client.dto.auth.AuthResetPasswordRequest;
import site.xlinks.ai.router.client.dto.auth.RsaPublicKeyResponse;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendRequest;
import site.xlinks.ai.router.client.dto.auth.VerifyCodeSendResponse;
import site.xlinks.ai.router.client.service.CustomerAccountService;
import site.xlinks.ai.router.client.service.VerifyCodeService;
import site.xlinks.ai.router.client.service.verifycode.VerifyCodeSender;
import site.xlinks.ai.router.client.service.verifycode.VerifyCodeSenderFactory;
import site.xlinks.ai.router.common.result.Result;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final CustomerAccountService customerAccountService;
    private final VerifyCodeService verifyCodeService;
    private final VerifyCodeSenderFactory verifyCodeSenderFactory;

    @PostMapping("/rsa-public-key")
    public Result<RsaPublicKeyResponse> getRsaPublicKey() {
        RsaPublicKeyResponse response = new RsaPublicKeyResponse();
        response.setAlgorithm("RSA/ECB/PKCS1Padding");
        response.setPublicKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAmockPublicKeyForClientModule");
        return Result.success(response);
    }

    @PostMapping("/verify-code")
    public Result<VerifyCodeSendResponse> sendVerifyCode(@Valid @RequestBody VerifyCodeSendRequest request) {
        customerAccountService.validateVerifyCodeTarget(request.getScene(), request.getCodeType(), request.getTarget());

        VerifyCodeService.VerifyCodeIssueResult issued = verifyCodeService.issueCode(
                request.getScene(),
                request.getCodeType(),
                request.getTarget()
        );

        VerifyCodeSender sender = verifyCodeSenderFactory.getSender(request.getCodeType());
        VerifyCodeSendResponse response = sender.send(request.getScene(), request.getTarget(), issued.token(), issued.expireSeconds());
        return Result.success(response);
    }

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody AuthRegisterRequest request) {
        customerAccountService.register(request);
        return Result.success("注册成功", null);
    }

    @PostMapping("/reset-password")
    public Result<String> resetPassword(@Valid @RequestBody AuthResetPasswordRequest request) {
        customerAccountService.resetPassword(request);
        return Result.success("密码重置成功", null);
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