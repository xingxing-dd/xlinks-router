package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.mapper.ModelMapper;

/**
 * Standard model service.
 */
@Service
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    public IPage<Model> pageQuery(Integer page, Integer pageSize, String modelCode, Integer status) {
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(modelCode), Model::getModelCode, modelCode)
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
        validateUnique(model.getModelCode(), null);
        if (model.getCacheHitPrice() == null) {
            model.setCacheHitPrice(model.getInputPrice());
        }
        return super.save(model);
    }

    public boolean update(Model model) {
        Model existing = getById(model.getId());
        String modelCode = StringUtils.hasText(model.getModelCode()) ? model.getModelCode() : existing.getModelCode();
        validateUnique(modelCode, model.getId());
        if (model.getCacheHitPrice() == null && existing.getCacheHitPrice() == null && model.getInputPrice() != null) {
            model.setCacheHitPrice(model.getInputPrice());
        }
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

    private void validateUnique(String modelCode, Long excludeId) {
        if (!StringUtils.hasText(modelCode)) {
            return;
        }
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<Model>()
                .eq(Model::getModelCode, modelCode);
        if (excludeId != null) {
            wrapper.ne(Model::getId, excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model code already exists under this endpoint");
        }
    }
}
