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
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.mapper.ModelEndpointMapper;
import site.xlinks.ai.router.mapper.ModelMapper;

/**
 * Standard model service.
 */
@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    private final ModelEndpointMapper modelEndpointMapper;

    public IPage<Model> pageQuery(Integer page, Integer pageSize, Long endpointId,
                                  String modelCode, Integer status) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(endpointId != null, Model::getEndpointId, endpointId)
                .like(StringUtils.hasText(modelCode), Model::getModelCode, modelCode)
                .eq(status != null, Model::getStatus, status)
                .orderByDesc(Model::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public Model getById(Long id) {
        Model model = super.getById(id);
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model not found");
        }
        return model;
    }

    public boolean save(Model model) {
        validateEndpoint(model.getEndpointId());
        validateUnique(model.getEndpointId(), model.getModelCode(), null);
        return super.save(model);
    }

    public boolean update(Model model) {
        Model existing = getById(model.getId());
        Long endpointId = model.getEndpointId() != null ? model.getEndpointId() : existing.getEndpointId();
        String modelCode = StringUtils.hasText(model.getModelCode()) ? model.getModelCode() : existing.getModelCode();
        validateEndpoint(endpointId);
        validateUnique(endpointId, modelCode, model.getId());
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

    private void validateEndpoint(Long endpointId) {
        if (endpointId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Endpoint ID must not be null");
        }
        ModelEndpoint endpoint = modelEndpointMapper.selectById(endpointId);
        if (endpoint == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model endpoint not found");
        }
    }

    private void validateUnique(Long endpointId, String modelCode, Long excludeId) {
        if (endpointId == null || !StringUtils.hasText(modelCode)) {
            return;
        }
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<Model>()
                .eq(Model::getEndpointId, endpointId)
                .eq(Model::getModelCode, modelCode);
        if (excludeId != null) {
            wrapper.ne(Model::getId, excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model code already exists under this endpoint");
        }
    }
}
