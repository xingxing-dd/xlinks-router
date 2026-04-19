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
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.mapper.ModelMapper;

/**
 * Standard model service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelService extends ServiceImpl<ModelMapper, Model> {

    private final ApiCacheRefreshNotifier apiCacheRefreshNotifier;

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
        normalizeModelProvider(model, true);
        if (model.getCacheHitPrice() == null) {
            model.setCacheHitPrice(model.getInputPrice());
        }
        boolean saved = super.save(model);
        if (saved) {
            log.info("Model created: id={}, modelCode={}", model.getId(), model.getModelCode());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("model", "created", model.getId());
        }
        return saved;
    }

    public boolean update(Model model) {
        Model existing = getById(model.getId());
        String modelCode = StringUtils.hasText(model.getModelCode()) ? model.getModelCode() : existing.getModelCode();
        validateUnique(modelCode, model.getId());
        normalizeModelProvider(model, false);
        if (model.getCacheHitPrice() == null && existing.getCacheHitPrice() == null && model.getInputPrice() != null) {
            model.setCacheHitPrice(model.getInputPrice());
        }
        boolean updated = super.updateById(model);
        if (updated) {
            log.info("Model updated: id={}, modelCode={}", model.getId(), modelCode);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("model", "updated", model.getId());
        }
        return updated;
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        Model model = new Model();
        model.setId(id);
        model.setStatus(status);
        boolean updated = super.updateById(model);
        if (updated) {
            log.info("Model status updated: id={}, status={}", id, status);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("model", "updated", id);
        }
        return updated;
    }

    public boolean deleteById(Long id) {
        getById(id);
        boolean deleted = super.removeById(id);
        if (deleted) {
            log.info("Model deleted: id={}", id);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("model", "deleted", id);
        }
        return deleted;
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

    private void normalizeModelProvider(Model model, boolean required) {
        if (model == null) {
            return;
        }
        if (required) {
            if (!StringUtils.hasText(model.getModelProvider())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Model provider must not be blank");
            }
            model.setModelProvider(model.getModelProvider().trim());
            return;
        }
        if (model.getModelProvider() != null) {
            if (!StringUtils.hasText(model.getModelProvider())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Model provider must not be blank");
            }
            model.setModelProvider(model.getModelProvider().trim());
        }
    }
}
