package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
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
import site.xlinks.ai.router.dto.ProviderModelBatchCreateDTO;
import site.xlinks.ai.router.vo.ProviderModelBatchCreateVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        ProviderModel existing = baseMapper.selectIncludingDeletedByProviderAndModel(
                providerModel.getProviderId(),
                providerModel.getModelId()
        );
        if (existing != null) {
            if (isDeleted(existing)) {
                boolean restored = restoreDeletedMapping(existing, providerModel);
                if (restored) {
                    providerModel.setId(existing.getId());
                    log.info("Provider model mapping restored: id={}, providerId={}, modelId={}",
                            existing.getId(), existing.getProviderId(), existing.getModelId());
                    apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "updated", existing.getId());
                }
                return restored;
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider and standard model mapping already exists");
        }

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

    @Transactional(rollbackFor = Exception.class)
    public ProviderModelBatchCreateVO batchCreate(ProviderModelBatchCreateDTO dto) {
        if (dto.getProviderId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider ID must not be null");
        }
        Provider provider = providerMapper.selectById(dto.getProviderId());
        if (provider == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider not found");
        }

        List<Long> normalizedModelIds = dto.getModelIds() == null
                ? List.of()
                : dto.getModelIds().stream()
                .filter(java.util.Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
        if (normalizedModelIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Standard model IDs must not be empty");
        }

        List<Model> models = modelMapper.selectBatchIds(normalizedModelIds);
        Map<Long, Model> modelMap = models.stream().collect(Collectors.toMap(Model::getId, item -> item));
        List<Long> missingModelIds = normalizedModelIds.stream()
                .filter(modelId -> !modelMap.containsKey(modelId))
                .collect(Collectors.toList());
        if (!missingModelIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Standard model not found: " + missingModelIds);
        }

        List<ProviderModel> existingMappings = baseMapper.selectIncludingDeletedByProviderAndModels(
                dto.getProviderId(),
                normalizedModelIds
        );
        Map<Long, ProviderModel> existingMappingByModelId = new HashMap<>();
        for (ProviderModel existingMapping : existingMappings) {
            if (existingMapping != null && existingMapping.getModelId() != null) {
                existingMappingByModelId.putIfAbsent(existingMapping.getModelId(), existingMapping);
            }
        }

        Integer targetStatus = dto.getStatus() == null ? 1 : dto.getStatus();
        List<ProviderModel> toCreate = new ArrayList<>();
        int restoredCount = 0;
        for (Long modelId : normalizedModelIds) {
            ProviderModel existingMapping = existingMappingByModelId.get(modelId);
            if (existingMapping != null) {
                if (isDeleted(existingMapping)) {
                    Model model = modelMap.get(modelId);
                    ProviderModel mapping = new ProviderModel();
                    mapping.setProviderId(dto.getProviderId());
                    mapping.setModelId(modelId);
                    mapping.setProviderModelCode(model.getModelCode());
                    mapping.setProviderModelName(StringUtils.hasText(model.getModelName()) ? model.getModelName() : model.getModelCode());
                    mapping.setStatus(targetStatus);
                    mapping.setRemark(dto.getRemark());
                    if (restoreDeletedMapping(existingMapping, mapping)) {
                        restoredCount++;
                    }
                }
                continue;
            }
            Model model = modelMap.get(modelId);
            ProviderModel mapping = new ProviderModel();
            mapping.setProviderId(dto.getProviderId());
            mapping.setModelId(modelId);
            mapping.setProviderModelCode(model.getModelCode());
            mapping.setProviderModelName(StringUtils.hasText(model.getModelName()) ? model.getModelName() : model.getModelCode());
            mapping.setStatus(targetStatus);
            mapping.setRemark(dto.getRemark());
            toCreate.add(mapping);
        }

        if (!toCreate.isEmpty()) {
            super.saveBatch(toCreate);
        }
        if (!toCreate.isEmpty() || restoredCount > 0) {
            apiCacheRefreshNotifier.notifyAdminCacheChanged("providerModel", "batchCreated", dto.getProviderId());
        }

        int requestedCount = normalizedModelIds.size();
        int createdCount = toCreate.size() + restoredCount;
        int skippedCount = requestedCount - createdCount;
        log.info("Provider model mappings batch create finished: providerId={}, requested={}, created={}, restored={}, skipped={}",
                dto.getProviderId(), requestedCount, createdCount, restoredCount, skippedCount);

        return new ProviderModelBatchCreateVO(requestedCount, createdCount, skippedCount);
    }

    private void validateUnique(Long providerId, Long modelId, String providerModelCode, Long excludeId) {
        ProviderModel existing = baseMapper.selectIncludingDeletedByProviderAndModel(providerId, modelId);
        if (existing != null && (excludeId == null || !excludeId.equals(existing.getId()))) {
            if (isDeleted(existing)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Provider and standard model mapping already exists in deleted state, please recreate it from add flow");
            }
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

    private boolean restoreDeletedMapping(ProviderModel existing, ProviderModel incoming) {
        Integer targetStatus = incoming.getStatus() == null ? 1 : incoming.getStatus();
        String providerModelName = incoming.getProviderModelName();
        if (!StringUtils.hasText(providerModelName)) {
            providerModelName = incoming.getProviderModelCode();
        }
        int updated = baseMapper.restoreDeleted(
                existing.getId(),
                incoming.getProviderModelCode(),
                providerModelName,
                targetStatus,
                incoming.getRemark(),
                incoming.getUpdateBy()
        );
        return updated > 0;
    }

    private boolean isDeleted(ProviderModel providerModel) {
        return providerModel != null && providerModel.getDeleted() != null && providerModel.getDeleted() == 1;
    }
}
