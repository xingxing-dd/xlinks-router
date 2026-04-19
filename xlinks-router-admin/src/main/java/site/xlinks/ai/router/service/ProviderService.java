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
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.ProviderMapper;

/**
 * Provider service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderService extends ServiceImpl<ProviderMapper, Provider> {

    private final ApiCacheRefreshNotifier apiCacheRefreshNotifier;

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
        boolean saved = super.save(provider);
        if (saved) {
            log.info("Provider created: id={}, providerCode={}", provider.getId(), provider.getProviderCode());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("provider", "created", provider.getId());
        }
        return saved;
    }

    public boolean update(Provider provider) {
        getById(provider.getId());
        boolean updated = super.updateById(provider);
        if (updated) {
            log.info("Provider updated: id={}", provider.getId());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("provider", "updated", provider.getId());
        }
        return updated;
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        Provider provider = new Provider();
        provider.setId(id);
        provider.setStatus(status);
        boolean updated = super.updateById(provider);
        if (updated) {
            log.info("Provider status updated: id={}, status={}", id, status);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("provider", "updated", id);
        }
        return updated;
    }

    public boolean deleteById(Long id) {
        getById(id);
        boolean deleted = super.removeById(id);
        if (deleted) {
            log.info("Provider deleted: id={}", id);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("provider", "deleted", id);
        }
        return deleted;
    }
}
