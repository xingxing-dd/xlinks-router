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
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.mapper.ModelMapper;

/**
 * Model Service
 */
@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    public IPage<Model> pageQuery(Integer page, Integer pageSize, Long providerId, Long endpointId,
                                   String modelCode, Integer status) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, Model::getProviderId, providerId)
               .eq(endpointId != null, Model::getEndpointId, endpointId)
               .like(StringUtils.hasText(modelCode), Model::getModelCode, modelCode)
               .eq(status != null, Model::getStatus, status)
               .orderByDesc(Model::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public Model getById(Long id) {
        Model model = super.getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model 不存在");
        }
        return model;
    }

    public boolean save(Model model) {
        return super.save(model);
    }

    public boolean update(Model model) {
        getById(model.getId());
        return super.updateById(model);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        Model model = new Model();
        model.setId(id);
        model.setStatus(status);
        return super.updateById(model);
    }

    public boolean deleteById(Long id) {
        getById(id);
        return super.removeById(id);
    }
}
