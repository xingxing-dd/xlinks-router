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
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;

/**
 * Provider model mapping service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderModelService extends ServiceImpl<ProviderModelMapper, ProviderModel> {

    private final ProviderMapper providerMapper;
    private final ModelMapper modelMapper;
    private final ApiCacheRefreshNotifier apiCacheRefreshNotifier;

    public IPage<ProviderModel> pageQuery(Integer page,
                                          Integer pageSize,
                                          Long providerId,
                                          Long modelId,
                                          String providerModelCode,
                                          Integer status) {
        LambdaQueryWrapper<ProviderModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, ProviderModel::getProviderId, providerId)
                .eq(modelId != null, ProviderModel::getModelId, modelId)
                .like(StringUtils.hasText(providerModelCode), ProviderModel::getProviderModelCode, providerModelCode)
                .eq(status != null, ProviderModel::getStatus, status)
                .orderByDesc(ProviderModel::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public ProviderModel getById(Long id) {
        ProviderModel providerModel = super.getById(id);
        if (providerModel == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider model mapping not found");
        }
        return providerModel;
    }

    public boolean save(ProviderModel providerModel) {
        validateReferences(providerModel.getProviderId(), providerModel.getModelId());
        validateUnique(providerModel.getProviderId(), providerModel.getModelId(), providerModel.getProviderModelCode(), null);
        boolean saved = super.save(providerModel);
        if (saved) {
            log.info("Provider model mapping created: id={}, providerId={}, modelId={}",
                    providerModel.getId(), providerModel.getProviderId(), providerModel.getModelId());
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "created", providerModel.getId());
        }
        return saved;
    }

    public boolean update(ProviderModel providerModel) {
        ProviderModel existing = getById(providerModel.getId());
        Long providerId = providerModel.getProviderId() != null ? providerModel.getProviderId() : existing.getProviderId();
        Long modelId = providerModel.getModelId() != null ? providerModel.getModelId() : existing.getModelId();
        String providerModelCode = providerModel.getProviderModelCode() != null
                ? providerModel.getProviderModelCode()
                : existing.getProviderModelCode();
        validateReferences(providerId, modelId);
        validateUnique(providerId, modelId, providerModelCode, providerModel.getId());
        boolean updated = super.updateById(providerModel);
        if (updated) {
            log.info("Provider model mapping updated: id={}, providerId={}, modelId={}",
                    providerModel.getId(), providerId, modelId);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "updated", providerModel.getId());
        }
        return updated;
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        ProviderModel providerModel = new ProviderModel();
        providerModel.setId(id);
        providerModel.setStatus(status);
        boolean updated = super.updateById(providerModel);
        if (updated) {
            log.info("Provider model mapping status updated: id={}, status={}", id, status);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "updated", id);
        }
        return updated;
    }

    public boolean deleteById(Long id) {
        getById(id);
        boolean deleted = super.removeById(id);
        if (deleted) {
            log.info("Provider model mapping deleted: id={}", id);
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "deleted", id);
        }
        return deleted;
    }

    private void validateUnique(Long providerId, Long modelId, String providerModelCode, Long excludeId) {
        LambdaQueryWrapper<ProviderModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(providerId != null, ProviderModel::getProviderId, providerId)
                .eq(modelId != null, ProviderModel::getModelId, modelId);
        if (excludeId != null) {
            wrapper.ne(ProviderModel::getId, excludeId);
        }
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider and standard model mapping already exists");
        }

        // Keep provider_model_code reusable under a provider:
        // one upstream model code can be shared by multiple standard models/endpoints.
    }

    private void validateReferences(Long providerId, Long modelId) {
        if (providerId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider ID must not be null");
        }
        if (modelId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model ID must not be null");
        }
        Provider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider not found");
        }
        Model model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Standard model not found");
        }
    }
}
