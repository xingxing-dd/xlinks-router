package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.CustomerModelMapping;
import site.xlinks.ai.router.mapper.CustomerModelMappingMapper;

/**
 * Customer Model Mapping Service
 */
@Service
@RequiredArgsConstructor
public class CustomerModelMappingService extends ServiceImpl<CustomerModelMappingMapper, CustomerModelMapping> {

    public IPage<CustomerModelMapping> pageQuery(Integer page, Integer pageSize, 
                                                         Long customerModelId, Long providerModelId, Integer status) {
        LambdaQueryWrapper<CustomerModelMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(customerModelId != null, CustomerModelMapping::getCustomerModelId, customerModelId)
               .eq(providerModelId != null, CustomerModelMapping::getProviderModelId, providerModelId)
               .eq(status != null, CustomerModelMapping::getStatus, status)
               .orderByAsc(CustomerModelMapping::getPriority);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public CustomerModelMapping getById(Long id) {
        CustomerModelMapping relation = super.getById(id);
        if (relation == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "关联关系不存在");
        }
        return relation;
    }

    public boolean save(CustomerModelMapping relation) {
        return super.save(relation);
    }

    public boolean update(CustomerModelMapping relation) {
        getById(relation.getId());
        return super.updateById(relation);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        CustomerModelMapping relation = new CustomerModelMapping();
        relation.setId(id);
        relation.setStatus(status);
        return super.updateById(relation);
    }
}
