package site.xlinks.ai.router.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.context.AdminAccountContext;
import site.xlinks.ai.router.dto.AdminLoginDTO;
import site.xlinks.ai.router.service.AdminAccountService;
import site.xlinks.ai.router.service.AdminTokenService;
import site.xlinks.ai.router.vo.AdminAccountProfileVO;
import site.xlinks.ai.router.vo.AdminLoginVO;

/**
 * Admin authentication API.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAccountService adminAccountService;
    private final AdminTokenService adminTokenService;

    @PostMapping("/login")
    public Result<AdminLoginVO> login(@Valid @RequestBody AdminLoginDTO dto) {
        return Result.success(adminAccountService.login(dto.getUsername(), dto.getPassword()));
    }

    @GetMapping("/me")
    public Result<AdminAccountProfileVO> me() {
        return Result.success(adminAccountService.getProfile(AdminAccountContext.requireAccount().getId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        adminTokenService.logout(AdminAccountContext.getAccountId());
        return Result.success();
    }
}
