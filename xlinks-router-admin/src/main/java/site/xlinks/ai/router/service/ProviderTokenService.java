package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

/**
 * Provider token service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenService extends ServiceImpl<ProviderTokenMapper, ProviderToken> {

    private final ProviderMapper providerMapper;
    private final ApiCacheRefreshNotifier apiCacheRefreshNotifier;

    public IPage<ProviderToken> pageQuery(Integer page, Integer pageSize, Long providerId, Integer tokenStatus) {
        LambdaQueryWrapper<ProviderToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, ProviderToken::getProviderId, providerId)
                .eq(tokenStatus != null, ProviderToken::getTokenStatus, tokenStatus)
                .orderByDesc(ProviderToken::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public ProviderToken getById(Long id) {
        ProviderToken token = super.getById(id);
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider token not found");
        }
        return token;
    }

    public boolean save(ProviderToken token) {
        validateProvider(token.getProviderId());
        boolean saved = super.save(token);
        if (saved) {
            log.info("Provider token created: id={}, providerId={}", token.getId(), token.getProviderId());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerToken", "created", token.getId());
        }
        return saved;
    }

    public boolean update(ProviderToken token) {
        ProviderToken existing = getById(token.getId());
        Long providerId = token.getProviderId() != null ? token.getProviderId() : existing.getProviderId();
        validateProvider(providerId);
        boolean updated = super.updateById(token);
        if (updated) {
            log.info("Provider token updated: id={}, providerId={}", token.getId(), providerId);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerToken", "updated", token.getId());
        }
        return updated;
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        ProviderToken token = new ProviderToken();
        token.setId(id);
        token.setTokenStatus(status);
        boolean updated = super.updateById(token);
        if (updated) {
            log.info("Provider token status updated: id={}, status={}", id, status);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerToken", "updated", id);
        }
        return updated;
    }

    public boolean deleteById(Long id) {
        getById(id);
        boolean deleted = super.removeById(id);
        if (deleted) {
            log.info("Provider token deleted: id={}", id);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerToken", "deleted", id);
        }
        return deleted;
    }

    private void validateProvider(Long providerId) {
        if (providerId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider ID must not be null");
        }
        Provider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider not found");
        }
    }
}
