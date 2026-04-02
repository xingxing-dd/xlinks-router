package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.config.AdminAuthProperties;
import site.xlinks.ai.router.entity.AdminAccount;
import site.xlinks.ai.router.mapper.AdminAccountMapper;
import site.xlinks.ai.router.vo.AdminAccountProfileVO;
import site.xlinks.ai.router.vo.AdminLoginVO;

import java.time.LocalDateTime;

/**
 * Admin account service.
 */
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private final AdminAccountMapper adminAccountMapper;
    private final AdminTokenService adminTokenService;
    private final AdminAuthProperties adminAuthProperties;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminLoginVO login(String username, String password) {
        AdminAccount account = getByUsername(username);
        if (account == null || !passwordEncoder.matches(password, account.getPassword())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "Administrator username or password is incorrect");
        }
        if (!Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Administrator account is disabled");
        }

        AdminAccount update = new AdminAccount();
        update.setId(account.getId());
        update.setLastLoginAt(LocalDateTime.now());
        adminAccountMapper.updateById(update);
        account.setLastLoginAt(update.getLastLoginAt());

        AdminLoginVO response = new AdminLoginVO();
        response.setAccessToken(adminTokenService.generateToken(account));
        response.setTokenType("Bearer");
        response.setExpiresIn(adminTokenService.getExpireSeconds());
        response.setUser(toProfile(account));
        return response;
    }

    public AdminAccount getById(Long id) {
        AdminAccount account = adminAccountMapper.selectById(id);
        if (account == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Administrator account not found");
        }
        return account;
    }

    public AdminAccountProfileVO getProfile(Long id) {
        return toProfile(getById(id));
    }

    public void bootstrapDefaultAdminIfNecessary() {
        if (adminAccountMapper.selectCount(null) > 0) {
            return;
        }
        AdminAccount account = new AdminAccount();
        account.setUsername(adminAuthProperties.getBootstrapUsername());
        account.setDisplayName(adminAuthProperties.getBootstrapDisplayName());
        account.setPassword(passwordEncoder.encode(adminAuthProperties.getBootstrapPassword()));
        account.setStatus(1);
        account.setRemark("Bootstrap administrator account");
        adminAccountMapper.insert(account);
    }

    private AdminAccount getByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return adminAccountMapper.selectOne(new LambdaQueryWrapper<AdminAccount>()
                .eq(AdminAccount::getUsername, username.trim()));
    }

    private AdminAccountProfileVO toProfile(AdminAccount account) {
        AdminAccountProfileVO profile = new AdminAccountProfileVO();
        profile.setId(account.getId());
        profile.setUsername(account.getUsername());
        profile.setDisplayName(account.getDisplayName());
        profile.setEmail(account.getEmail());
        profile.setPhone(account.getPhone());
        profile.setStatus(account.getStatus());
        profile.setLastLoginAt(account.getLastLoginAt());
        return profile;
    }
}
