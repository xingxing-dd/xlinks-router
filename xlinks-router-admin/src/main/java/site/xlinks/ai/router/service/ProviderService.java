package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.ProviderMapper;

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
        return super.save(provider);
    }

    public boolean update(Provider provider) {
        getById(provider.getId());
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
}
