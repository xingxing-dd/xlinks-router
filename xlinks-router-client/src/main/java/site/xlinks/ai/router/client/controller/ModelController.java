package site.xlinks.ai.router.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.client.dto.model.AvailableModelItemResponse;
import site.xlinks.ai.router.client.dto.model.CustomerModelItemResponse;
import site.xlinks.ai.router.client.dto.model.ModelDetailResponse;
import site.xlinks.ai.router.client.dto.model.ModelRouteItemResponse;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ModelController {

    private static final String PRICE_SUFFIX = "/M";

    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;
    private final ProviderModelMapper providerModelMapper;

    @GetMapping("/customer-models")
    public Result<PageResult<CustomerModelItemResponse>> getCustomerModels(@RequestParam(defaultValue = "1") Integer page,
                                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        List<CustomerModelItemResponse> records = List.of(
                new CustomerModelItemResponse(1L, "claude-sonnet", "Claude Sonnet", "chat", 1, 1, "primary model", "2026-03-08 10:00:00"),
                new CustomerModelItemResponse(2L, "claude-haiku", "Claude Haiku", "chat", 1, 0, "fast response model", "2026-03-10 14:20:00")
        );
        return Result.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @GetMapping("/models/available")
    public Result<List<AvailableModelItemResponse>> getAvailableModels() {
        List<Model> models = modelMapper.selectList(new LambdaQueryWrapper<Model>()
                .eq(Model::getStatus, 1)
                .eq(Model::getDeleted, 0)
                .orderByAsc(Model::getId));
        if (models == null || models.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        Map<Long, List<ProviderModel>> routesByModelId = loadProviderModelsByModelId(models.stream()
                .map(Model::getId)
                .collect(Collectors.toSet()));
        Map<Long, Provider> providerMap = loadProviders(routesByModelId.values().stream()
                .flatMap(List::stream)
                .map(ProviderModel::getProviderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet()));

        Map<Long, List<ProviderModel>> availableRoutesByModelId = filterRoutesByActiveProviders(routesByModelId, providerMap.keySet());

        List<AvailableModelItemResponse> responses = models.stream()
                .filter(model -> {
                    List<ProviderModel> routes = availableRoutesByModelId.get(model.getId());
                    return routes != null && !routes.isEmpty();
                })
                .map(model -> new AvailableModelItemResponse(
                        model.getId(),
                        model.getModelCode() == null ? model.getModelName() : model.getModelCode(),
                        resolveModelProvider(model),
                        model.getModelDesc(),
                        formatPrice(model.getInputPrice()),
                        formatPrice(model.getOutputPrice()),
                        formatPrice(model.getCacheHitPrice()),
                        formatContextWindow(model.getContextSize()),
                        model.getStatus() != null && model.getStatus() == 1 ? "available" : "unavailable"
                ))
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @GetMapping("/models/{id}")
    public Result<ModelDetailResponse> getModelDetail(@PathVariable Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null || model.getDeleted() != null && model.getDeleted() == 1) {
            return Result.success(null);
        }

        List<ProviderModel> providerModels = providerModelMapper.selectList(new LambdaQueryWrapper<ProviderModel>()
                .eq(ProviderModel::getModelId, id)
                .eq(ProviderModel::getDeleted, 0)
                .eq(ProviderModel::getStatus, 1));
        Map<Long, Provider> providerMap = loadProviders(providerModels.stream()
                .map(ProviderModel::getProviderId)
                .filter(providerId -> providerId != null && providerId > 0)
                .collect(Collectors.toSet()));

        List<ModelRouteItemResponse> routes = providerModels.stream()
                .filter(route -> providerMap.containsKey(route.getProviderId()))
                .map(route -> new ModelRouteItemResponse(
                        route.getProviderId(),
                        resolveProviderName(providerMap.get(route.getProviderId())),
                        route.getProviderModelCode() == null ? route.getProviderModelName() : route.getProviderModelCode(),
                        resolvePriority(providerMap.get(route.getProviderId()))
                ))
                .sorted((left, right) -> Integer.compare(
                        right.getPriority() == null ? 0 : right.getPriority(),
                        left.getPriority() == null ? 0 : left.getPriority()))
                .collect(Collectors.toList());

        ModelDetailResponse response = new ModelDetailResponse();
        response.setId(model.getId());
        response.setName(model.getModelCode() == null ? model.getModelName() : model.getModelCode());
        response.setProvider(resolveModelProvider(model));
        response.setDescription(model.getModelDesc());
        response.setInputPrice(formatPrice(model.getInputPrice()));
        response.setOutputPrice(formatPrice(model.getOutputPrice()));
        response.setCacheHitPrice(formatPrice(model.getCacheHitPrice()));
        response.setContextWindow(formatContextWindow(model.getContextSize()));
        response.setRoutes(routes);
        return Result.success(response);
    }

    private Map<Long, List<ProviderModel>> loadProviderModelsByModelId(Set<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProviderModel> providerModels = providerModelMapper.selectList(new LambdaQueryWrapper<ProviderModel>()
                .in(ProviderModel::getModelId, modelIds)
                .eq(ProviderModel::getStatus, 1)
                .eq(ProviderModel::getDeleted, 0));
        if (providerModels == null || providerModels.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ProviderModel>> result = new HashMap<>();
        for (ProviderModel providerModel : providerModels) {
            result.computeIfAbsent(providerModel.getModelId(), key -> new ArrayList<>()).add(providerModel);
        }
        return result;
    }

    private Map<Long, Provider> loadProviders(Collection<Long> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Provider> providers = providerMapper.selectList(new LambdaQueryWrapper<Provider>()
                .in(Provider::getId, providerIds)
                .eq(Provider::getStatus, 1)
                .eq(Provider::getDeleted, 0));
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Provider> providerMap = new HashMap<>();
        for (Provider provider : providers) {
            providerMap.put(provider.getId(), provider);
        }
        return providerMap;
    }

    private String resolvePreferredProviderName(List<ProviderModel> routes, Map<Long, Provider> providerMap) {
        if (routes == null || routes.isEmpty()) {
            return "";
        }
        ProviderModel preferred = routes.stream()
                .sorted((left, right) -> Integer.compare(
                        resolvePriority(providerMap.get(right.getProviderId())),
                        resolvePriority(providerMap.get(left.getProviderId()))))
                .findFirst()
                .orElse(null);
        if (preferred == null) {
            return "";
        }
        return resolveProviderName(providerMap.get(preferred.getProviderId()));
    }

    private Map<Long, List<ProviderModel>> filterRoutesByActiveProviders(Map<Long, List<ProviderModel>> routesByModelId,
                                                                          Set<Long> activeProviderIds) {
        if (routesByModelId == null || routesByModelId.isEmpty() || activeProviderIds == null || activeProviderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<ProviderModel>> result = new HashMap<>();
        for (Map.Entry<Long, List<ProviderModel>> entry : routesByModelId.entrySet()) {
            List<ProviderModel> filteredRoutes = entry.getValue().stream()
                    .filter(route -> route.getProviderId() != null && activeProviderIds.contains(route.getProviderId()))
                    .collect(Collectors.toList());
            if (!filteredRoutes.isEmpty()) {
                result.put(entry.getKey(), filteredRoutes);
            }
        }
        return result;
    }

    private String resolveModelProvider(Model model) {
        if (model == null || !StringUtils.hasText(model.getModelProvider())) {
            return "";
        }
        return model.getModelProvider().trim();
    }

    private String resolveProviderName(Provider provider) {
        if (provider == null) {
            return "";
        }
        if (provider.getProviderName() != null && !provider.getProviderName().isBlank()) {
            return provider.getProviderName();
        }
        return provider.getProviderCode() == null ? "" : provider.getProviderCode();
    }

    private int resolvePriority(Provider provider) {
        return provider == null || provider.getPriority() == null ? 0 : provider.getPriority();
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "$0" + PRICE_SUFFIX;
        }
        BigDecimal scaled = price.setScale(2, RoundingMode.HALF_UP);
        return "$" + scaled.toPlainString() + PRICE_SUFFIX;
    }

    private String formatContextWindow(Integer contextSize) {
        if (contextSize == null || contextSize <= 0) {
            return "-";
        }
        return contextSize + "K";
    }
}
