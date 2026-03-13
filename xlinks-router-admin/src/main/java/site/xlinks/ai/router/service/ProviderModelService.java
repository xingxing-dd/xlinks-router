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
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.mapper.ProviderModelMapper;

/**
 * Provider Model Service
 */
@Service
@RequiredArgsConstructor
public class ProviderModelService extends ServiceImpl<ProviderModelMapper, ProviderModel> {

    public IPage<ProviderModel> pageQuery(Integer page, Integer pageSize, Long providerId, 
                                          String providerModelCode, Integer status) {
        LambdaQueryWrapper<ProviderModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, ProviderModel::getProviderId, providerId)
               .like(StringUtils.hasText(providerModelCode), ProviderModel::getProviderModelCode, providerModelCode)
               .eq(status != null, ProviderModel::getStatus, status)
               .orderByDesc(ProviderModel::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public ProviderModel getById(Long id) {
        ProviderModel model = super.getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider Model 不存在");
        }
        return model;
    }

    public boolean save(ProviderModel model) {
        return super.save(model);
    }

    public boolean update(ProviderModel model) {
        getById(model.getId());
        return super.updateById(model);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        ProviderModel model = new ProviderModel();
        model.setId(id);
        model.setStatus(status);
        return super.updateById(model);
    }
}
