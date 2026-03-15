package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Provider Token 选择服务
 * 在目标 Provider 下选择可用且最优的 Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenSelectService {

    private final site.xlinks.ai.router.mapper.ProviderTokenMapper providerTokenMapper;

    /**
     * 选择可用的 Provider Token
     *
     * @param providerId Provider ID
     * @return 可用的 Provider Token
     */
    @Transactional
    public ProviderToken selectToken(Long providerId) {
        log.debug("Selecting token for provider: {}", providerId);

        // 1. 查询状态正常的 Token
        LambdaQueryWrapper<ProviderToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProviderToken::getProviderId, providerId)
               .eq(ProviderToken::getTokenStatus, 1)
               .gt(ProviderToken::getExpireTime, LocalDateTime.now())
               .or()
               .isNull(ProviderToken::getExpireTime);

        List<ProviderToken> tokens = providerTokenMapper.selectList(wrapper);

        if (tokens == null || tokens.isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "无可用的 Provider Token");
        }

        // 2. 过滤已过期和额度耗尽的 Token
        List<ProviderToken> availableTokens = tokens.stream()
                .filter(this::isAvailable)
                .collect(java.util.stream.Collectors.toList());

        if (availableTokens.isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "所有 Provider Token 都不可用");
        }

        // 3. 排序选择：优先选择额度剩余更多、最近最少使用的
        // 这里简化处理：直接取第一个可用的
        // 后续可扩展为更复杂的排序策略
        ProviderToken selected = availableTokens.get(0);

        // 4. 更新最后使用时间
        selected.setLastUsedAt(LocalDateTime.now());
        providerTokenMapper.updateById(selected);

        // 5. 更新已使用额度（如果需要）
        // 注意：实际额度更新应该在调用完成后更新，这里只是预留扩展点

        log.debug("Selected token: {} for provider: {}", selected.getId(), providerId);
        return selected;
    }

    /**
     * 检查 Token 是否可用
     */
    private boolean isAvailable(ProviderToken token) {
        // 检查状态
        if (token.getTokenStatus() == null || token.getTokenStatus() != 1) {
            return false;
        }

        // 检查过期时间
        LocalDateTime expireTime = token.getExpireTime();
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            return false;
        }

        // 检查额度（如果有额度限制）
        Long quotaTotal = token.getQuotaTotal();
        Long quotaUsed = token.getQuotaUsed();
        if (quotaTotal != null && quotaUsed != null && quotaUsed >= quotaTotal) {
            return false;
        }

        return true;
    }

    /**
     * 更新 Token 已使用额度
     *
     * @param tokenId     Token ID
     * @param usedTokens  使用的 Token 数量
     */
    @Transactional
    public void updateQuotaUsed(Long tokenId, Long usedTokens) {
        ProviderToken token = providerTokenMapper.selectById(tokenId);
        if (token != null) {
            Long currentUsed = token.getQuotaUsed();
            if (currentUsed == null) {
                currentUsed = 0L;
            }
            token.setQuotaUsed(currentUsed + usedTokens);
            providerTokenMapper.updateById(token);
        }
    }
}
