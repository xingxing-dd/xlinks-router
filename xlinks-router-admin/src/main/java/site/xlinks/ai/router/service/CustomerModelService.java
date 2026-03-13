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
import site.xlinks.ai.router.entity.CustomerModel;
import site.xlinks.ai.router.mapper.CustomerModelMapper;

/**
 * Customer Model Service
 */
@Service
@RequiredArgsConstructor
public class CustomerModelService extends ServiceImpl<CustomerModelMapper, CustomerModel> {

    public IPage<CustomerModel> pageQuery(Integer page, Integer pageSize, String logicModelCode, 
                                          String logicModelName, String modelType, Integer status) {
        LambdaQueryWrapper<CustomerModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(logicModelCode), CustomerModel::getLogicModelCode, logicModelCode)
               .like(StringUtils.hasText(logicModelName), CustomerModel::getLogicModelName, logicModelName)
               .eq(StringUtils.hasText(modelType), CustomerModel::getModelType, modelType)
               .eq(status != null, CustomerModel::getStatus, status)
               .orderByDesc(CustomerModel::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public CustomerModel getById(Long id) {
        CustomerModel model = super.getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Customer Model 不存在");
        }
        return model;
    }

    public CustomerModel getByCode(String logicModelCode) {
        LambdaQueryWrapper<CustomerModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerModel::getLogicModelCode, logicModelCode);
        return this.getOne(wrapper);
    }

    public boolean save(CustomerModel model) {
        CustomerModel exist = getByCode(model.getLogicModelCode());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "逻辑模型编码已存在");
        }
        return super.save(model);
    }

    public boolean update(CustomerModel model) {
        getById(model.getId());
        return super.updateById(model);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        CustomerModel model = new CustomerModel();
        model.setId(id);
        model.setStatus(status);
        return super.updateById(model);
    }
}
