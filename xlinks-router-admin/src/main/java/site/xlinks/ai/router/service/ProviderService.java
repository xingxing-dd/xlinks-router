package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.enums.ProviderCacheHitStrategy;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.ProviderMapper;

import java.util.Locale;

/**
 * Provider service.
 */
@Service
@RequiredArgsConstructor
public class ProviderService extends ServiceImpl<ProviderMapper, Provider> {

    public IPage<Provider> pageQuery(Integer page, Integer pageSize, String providerCode,
                                     String providerName, Integer status) {
        LambdaQueryWrapper<Provider> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(providerCode), Provider::getProviderCode, providerCode)
                .like(StringUtils.hasText(providerName), Provider::getProviderName, providerName)
                .eq(status != null, Provider::getStatus, status)
                .orderByDesc(Provider::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public Provider getById(Long id) {
        Provider provider = super.getById(id);
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider not found");
        }
        return provider;
    }

    public Provider getByCode(String providerCode) {
        return this.getOne(new LambdaQueryWrapper<Provider>()
                .eq(Provider::getProviderCode, providerCode));
    }

    public boolean save(Provider provider) {
        Provider exist = getByCode(provider.getProviderCode());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider code already exists");
        }
        provider.setCacheHitStrategy(normalizeCacheHitStrategy(provider.getCacheHitStrategy()));
        return super.save(provider);
    }

    public boolean update(Provider provider) {
        Provider existing = getById(provider.getId());
        if (provider.getCacheHitStrategy() != null) {
            provider.setCacheHitStrategy(normalizeCacheHitStrategy(provider.getCacheHitStrategy()));
        } else if (!StringUtils.hasText(existing.getCacheHitStrategy())) {
            provider.setCacheHitStrategy(ProviderCacheHitStrategy.NONE.getCode());
        }
        return super.updateById(provider);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        Provider provider = new Provider();
        provider.setId(id);
        provider.setStatus(status);
        return super.updateById(provider);
    }

    public boolean deleteById(Long id) {
        getById(id);
        return super.removeById(id);
    }

    private String normalizeCacheHitStrategy(String strategyCode) {
        if (!StringUtils.hasText(strategyCode)) {
            return ProviderCacheHitStrategy.NONE.getCode();
        }
        String normalized = strategyCode.trim().toLowerCase(Locale.ROOT);
        for (ProviderCacheHitStrategy strategy : ProviderCacheHitStrategy.values()) {
            if (strategy.getCode().equals(normalized)) {
                return strategy.getCode();
            }
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR,
                "Unsupported cacheHitStrategy: " + strategyCode);
    }
}
