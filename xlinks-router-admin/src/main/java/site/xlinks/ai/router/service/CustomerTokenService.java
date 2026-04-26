package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.UsageRecordAdminMapper;
import site.xlinks.ai.router.vo.CustomerTokenUsageStatsVO;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer token service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenService extends ServiceImpl<CustomerTokenMapper, CustomerToken> {

    private final CustomerAccountMapper customerAccountMapper;
    private final UsageRecordAdminMapper usageRecordAdminMapper;

    public IPage<CustomerToken> pageQuery(Integer page, Integer pageSize, String customerName, Integer status) {
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(customerName), CustomerToken::getCustomerName, customerName)
                .eq(status != null, CustomerToken::getStatus, status)
                .orderByDesc(CustomerToken::getCreatedAt);
        IPage<CustomerToken> tokenPage = this.page(new Page<>(page, pageSize), wrapper);
        enrichTokenUsage(tokenPage.getRecords());
        return tokenPage;
    }

    public CustomerToken getById(Long id) {
        CustomerToken token = super.getById(id);
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Customer token not found");
        }
        return token;
    }

    public CustomerToken getByTokenValue(String tokenValue) {
        String hash = hashToken(tokenValue);
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerToken::getTokenValue, hash);
        return this.getOne(wrapper);
    }

    public CustomerToken create(CustomerToken token) {
        CustomerAccount account = resolveAccount(token.getCustomerName());
        token.setAccountId(account.getId());
        token.setCustomerName(resolveDisplayName(account));
        token.setDailyQuota(normalizeDailyQuota(token.getDailyQuota()));
        token.setTotalQuota(normalizeDailyQuota(token.getTotalQuota()));
        token.setUsedQuota(BigDecimal.ZERO);
        token.setTotalUsedQuota(BigDecimal.ZERO);

        String rawToken = "xlr_ct_" + UUID.randomUUID().toString().replace("-", "");
        token.setTokenValue(hashToken(rawToken));
        super.save(token);

        token.setTokenValue(rawToken);
        return token;
    }

    public boolean update(CustomerToken token) {
        CustomerToken existing = getById(token.getId());
        if (StringUtils.hasText(token.getCustomerName())) {
            CustomerAccount account = resolveAccount(token.getCustomerName());
            token.setAccountId(account.getId());
            token.setCustomerName(resolveDisplayName(account));
        } else {
            token.setAccountId(existing.getAccountId());
        }
        token.setDailyQuota(normalizeDailyQuota(token.getDailyQuota()));
        token.setTotalQuota(normalizeDailyQuota(token.getTotalQuota()));
        token.setTokenValue(null);
        return super.updateById(token);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        CustomerToken token = new CustomerToken();
        token.setId(id);
        token.setStatus(status);
        return super.updateById(token);
    }

    public boolean deleteById(Long id) {
        getById(id);
        return super.removeById(id);
    }

    private CustomerAccount resolveAccount(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Customer identifier must not be blank");
        }
        LambdaQueryWrapper<CustomerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerAccount::getUsername, identifier)
                .or()
                .eq(CustomerAccount::getPhone, identifier)
                .or()
                .eq(CustomerAccount::getEmail, identifier);
        CustomerAccount account = customerAccountMapper.selectOne(wrapper);
        if (account == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Customer account not found");
        }
        return account;
    }

    private String resolveDisplayName(CustomerAccount account) {
        if (StringUtils.hasText(account.getUsername())) {
            return account.getUsername();
        }
        if (StringUtils.hasText(account.getEmail())) {
            return account.getEmail();
        }
        if (StringUtils.hasText(account.getPhone())) {
            return account.getPhone();
        }
        return String.valueOf(account.getId());
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private BigDecimal normalizeDailyQuota(BigDecimal dailyQuota) {
        if (dailyQuota == null) {
            return null;
        }
        if (dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return dailyQuota;
    }

    private void enrichTokenUsage(List<CustomerToken> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        records.forEach(record -> {
            record.setTodayUsedTokens(0L);
            record.setTotalUsedTokens(0L);
        });

        List<String> tokenHashes = records.stream()
                .map(CustomerToken::getTokenValue)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (tokenHashes.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();
        Map<String, CustomerTokenUsageStatsVO> usageByHash = usageRecordAdminMapper
                .aggregateCustomerTokenUsage(tokenHashes, startAt, endAt)
                .stream()
                .filter(item -> StringUtils.hasText(item.getTokenHash()))
                .collect(Collectors.toMap(CustomerTokenUsageStatsVO::getTokenHash, Function.identity(), (left, right) -> left));

        records.forEach(record -> {
            CustomerTokenUsageStatsVO usage = usageByHash.get(record.getTokenValue());
            if (usage == null) {
                return;
            }
            record.setTodayUsedTokens(usage.getTodayUsedTokens() == null ? 0L : usage.getTodayUsedTokens());
            record.setTotalUsedTokens(usage.getTotalUsedTokens() == null ? 0L : usage.getTotalUsedTokens());
        });
    }
}
