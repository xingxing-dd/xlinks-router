package site.xlinks.ai.router.merchant.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.merchant.config.AuthProperties;
import site.xlinks.ai.router.merchant.dto.AuthTokenVO;
import site.xlinks.ai.router.merchant.dto.RsaPublicKeyVO;
import site.xlinks.ai.router.merchant.dto.SmsCodeSendDTO;
import site.xlinks.ai.router.merchant.dto.SmsCodeSendResultVO;
import site.xlinks.ai.router.merchant.dto.UserLoginDTO;
import site.xlinks.ai.router.merchant.dto.UserRegisterDTO;
import site.xlinks.ai.router.merchant.service.AuthUserService;
import site.xlinks.ai.router.merchant.service.RsaCryptoService;
import site.xlinks.ai.router.merchant.service.SmsCodeService;

/**
 * 用户认证接口。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "邮箱密码注册登录与邮箱验证码接口")
public class AuthController {

    private final SmsCodeService smsCodeService;
    private final AuthUserService authUserService;
    private final RsaCryptoService rsaCryptoService;
    private final AuthProperties authProperties;

    @PostMapping("/rsa-public-key")
    @Operation(summary = "获取 RSA 公钥")
    public Result<RsaPublicKeyVO> getRsaPublicKey() {
        return Result.success(RsaPublicKeyVO.builder()
                .algorithm("RSA/ECB/PKCS1Padding")
                .publicKey(rsaCryptoService.getPublicKey())
                .build());
    }

    @PostMapping("/sms-code")
    @Operation(summary = "发送邮箱验证码")
    public Result<SmsCodeSendResultVO> sendSmsCode(@Valid @RequestBody SmsCodeSendDTO dto) {
        String code = smsCodeService.sendCode(dto.getEmail(), dto.getScene());
        return Result.success(SmsCodeSendResultVO.builder()
                .message("验证码发送成功")
                .mockCode(Boolean.TRUE.equals(authProperties.getSmsMockEnabled()) ? code : null)
                .expireSeconds(authProperties.getSmsCodeExpireSeconds())
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<String> register(@Valid @RequestBody UserRegisterDTO dto, HttpServletRequest request) {
        authUserService.register(dto, request);
        return Result.success("注册成功");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<AuthTokenVO> login(@Valid @RequestBody UserLoginDTO dto, HttpServletRequest request) {
        return Result.success(authUserService.login(dto, request));
    }
}