package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.context.RouteTarget;
import site.xlinks.ai.router.entity.CustomerModel;
import site.xlinks.ai.router.entity.CustomerModelMapping;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型路由服务
 * 根据客户模型和权益类型，路由到目标 Provider 和模型
 * 
 * MVP 阶段：直接查询 model_mapping + provider_model + provider
 * 优化阶段：可使用 model_route_cache 宽表提升性能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelRouteService {

    private final site.xlinks.ai.router.mapper.CustomerModelMapper customerModelMapper;
    private final site.xlinks.ai.router.mapper.CustomerModelMappingMapper customerModelMappingMapper;
    private final site.xlinks.ai.router.mapper.ProviderModelMapper providerModelMapper;
    private final site.xlinks.ai.router.mapper.ProviderMapper providerMapper;

    /**
     * 根据客户模型和权益类型获取路由目标
     *
     * @param customerModelCode 客户模型编码
     * @param usageType         权益类型：0-不限制，1-仅套餐，2-仅余额
     * @return 路由目标
     */
    public RouteTarget route(String customerModelCode, Integer usageType) {
        log.debug("Routing for model: {}, usageType: {}", customerModelCode, usageType);

        // 1. 查询客户模型
        LambdaQueryWrapper<CustomerModel> modelWrapper = new LambdaQueryWrapper<>();
        modelWrapper.eq(CustomerModel::getLogicModelCode, customerModelCode)
                    .eq(CustomerModel::getStatus, 1);
        CustomerModel customerModel = customerModelMapper.selectOne(modelWrapper);
        
        if (customerModel == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型不存在: " + customerModelCode);
        }

        // 2. 查询模型映射关系（获取可用的 Provider Model）
        LambdaQueryWrapper<CustomerModelMapping> mappingWrapper = new LambdaQueryWrapper<>();
        mappingWrapper.eq(CustomerModelMapping::getCustomerModelId, customerModel.getId())
                      .eq(CustomerModelMapping::getStatus, 1)
                      .orderByAsc(CustomerModelMapping::getPriority);
        List<CustomerModelMapping> mappings = customerModelMappingMapper.selectList(mappingWrapper);

        if (mappings == null || mappings.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型无可用路由: " + customerModelCode);
        }

        // 3. 根据 usageType 筛选合适的 Provider Model
        List<Long> providerModelIds = mappings.stream()
                .map(CustomerModelMapping::getProviderModelId)
                .collect(Collectors.toList());

        // 查询所有候选的 Provider Model
        LambdaQueryWrapper<ProviderModel> pmWrapper = new LambdaQueryWrapper<>();
        pmWrapper.in(ProviderModel::getId, providerModelIds)
                 .eq(ProviderModel::getStatus, 1);
        List<ProviderModel> providerModels = providerModelMapper.selectList(pmWrapper);

        // 4. 根据 usageType 过滤
        // usageType: 0-不限制, 1-仅套餐, 2-仅余额
        // provider_model.usage_type: 0-不限制, 1-仅套餐, 2-仅余额
        List<ProviderModel> filteredModels = filterByUsageType(providerModels, usageType);

        if (filteredModels.isEmpty()) {
            // 如果找不到合适的路由，尝试使用不限制的模型
            filteredModels = providerModels.stream()
                    .filter(pm -> pm.getUsageType() == null || pm.getUsageType() == 0)
                    .collect(Collectors.toList());
            
            if (filteredModels.isEmpty()) {
                throw new BusinessException(ErrorCode.ROUTE_ERROR, "无可用的路由配置");
            }
        }

        // 5. 按优先级排序，选择第一个
        // 注意：这里简化处理，直接使用 mapping 中的 priority
        ProviderModel selectedProviderModel = filteredModels.get(0);

        // 6. 查询 Provider 信息
        Provider provider = providerMapper.selectById(selectedProviderModel.getProviderId());
        if (provider == null || provider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "Provider 不可用");
        }

        // 7. 构建路由目标
        return RouteTarget.builder()
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerType(provider.getProviderType())
                .baseUrl(provider.getBaseUrl())
                .providerModel(selectedProviderModel.getProviderModelName())
                .priority(mappings.get(0).getPriority())
                .usageType(selectedProviderModel.getUsageType())
                .modelType(customerModel.getModelType())
                .build();
    }

    /**
     * 根据权益类型过滤 Provider Model
     */
    private List<ProviderModel> filterByUsageType(List<ProviderModel> models, Integer usageType) {
        if (models == null || models.isEmpty()) {
            return new ArrayList<>();
        }

        // 优先返回完全匹配的
        List<ProviderModel> matched = models.stream()
                .filter(pm -> {
                    Integer ut = pm.getUsageType();
                    if (ut == null || ut == 0) return true; // 不限制的始终可用
                    return ut.equals(usageType); // 精确匹配
                })
                .collect(Collectors.toList());

        if (!matched.isEmpty()) {
            return matched;
        }

        // 如果没有精确匹配的，返回不限制的
        return models.stream()
                .filter(pm -> pm.getUsageType() == null || pm.getUsageType() == 0)
                .collect(Collectors.toList());
    }
}
