package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.ProviderMapper;

/**
 * Provider Service
 */
@Service
@RequiredArgsConstructor
public class ProviderService extends ServiceImpl<ProviderMapper, Provider> {

    /**
     * 分页查询
     */
    public IPage<Provider> pageQuery(Integer page, Integer pageSize, String providerCode, 
                                      String providerName, Integer status) {
        LambdaQueryWrapper<Provider> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(providerCode), Provider::getProviderCode, providerCode)
               .like(StringUtils.hasText(providerName), Provider::getProviderName, providerName)
               .eq(status != null, Provider::getStatus, status)
               .orderByDesc(Provider::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 根据 ID 查询
     */
    public Provider getById(Long id) {
        Provider provider = super.getById(id);
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider 不存在");
        }
        return provider;
    }

    /**
     * 根据编码查询
     */
    public Provider getByCode(String providerCode) {
        LambdaQueryWrapper<Provider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Provider::getProviderCode, providerCode);
        return this.getOne(wrapper);
    }

    /**
     * 新增
     */
    public boolean save(Provider provider) {
        // 检查编码唯一性
        Provider exist = getByCode(provider.getProviderCode());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider 编码已存在");
        }
        return super.save(provider);
    }

    /**
     * 更新
     */
    public boolean update(Provider provider) {
        // 检查是否存在
        getById(provider.getId());
        return super.updateById(provider);
    }

    /**
     * 更新状态
     */
    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        Provider provider = new Provider();
        provider.setId(id);
        provider.setStatus(status);
        return super.updateById(provider);
    }
}
