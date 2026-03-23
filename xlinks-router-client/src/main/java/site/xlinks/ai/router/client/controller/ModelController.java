package site.xlinks.ai.router.client.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.model.AvailableModelItemResponse;
import site.xlinks.ai.router.client.dto.model.CustomerModelItemResponse;
import site.xlinks.ai.router.client.dto.model.ModelDetailResponse;
import site.xlinks.ai.router.client.dto.model.ModelRouteItemResponse;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ModelController {

    private static final String PRICE_SUFFIX = "/M";

    private final ModelMapper modelMapper;
    private final ProviderMapper providerMapper;

    @GetMapping("/customer-models")
    public Result<PageResult<CustomerModelItemResponse>> getCustomerModels(@RequestParam(defaultValue = "1") Integer page,
                                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        List<CustomerModelItemResponse> records = List.of(
                new CustomerModelItemResponse(1L, "claude-sonnet", "Claude Sonnet", "chat", 1, 1, "主力模型", "2026-03-08 10:00:00"),
                new CustomerModelItemResponse(2L, "claude-haiku", "Claude Haiku", "chat", 1, 0, "快速响应模型", "2026-03-10 14:20:00")
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
        Map<Long, Provider> providerMap = loadProviders(models.stream()
                .map(Model::getProviderId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet()));
        List<AvailableModelItemResponse> responses = models.stream()
                .map(model -> new AvailableModelItemResponse(
                        model.getId(),
                        model.getModelCode() == null ? model.getModelName() : model.getModelCode(),
                        resolveProviderName(providerMap.get(model.getProviderId())),
                        model.getModelDesc(),
                        formatPrice(model.getInputPrice()),
                        formatPrice(model.getOutputPrice()),
                        formatContextWindow(model.getContextSize()),
                        model.getStatus() != null && model.getStatus() == 1 ? "available" : "unavailable"
                ))
                .collect(Collectors.toList());
        return Result.success(responses);
    }

    @GetMapping("/models/{id}")
    public Result<ModelDetailResponse> getModelDetail(@PathVariable Long id) {
        ModelDetailResponse response = new ModelDetailResponse();
        response.setId(id);
        response.setName("claude-3-7-sonnet");
        response.setProvider("Anthropic");
        response.setDescription("高性能对话模型");
        response.setInputPrice("$3.00/M");
        response.setOutputPrice("$15.00/M");
        response.setContextWindow("200K");
        response.setRoutes(List.of(
                new ModelRouteItemResponse(1L, "OpenAI Compatible", "claude-3-7-sonnet-20250219", 1),
                new ModelRouteItemResponse(2L, "Anthropic Official", "claude-3-7-sonnet", 2)
        ));
        return Result.success(response);
    }

    private Map<Long, Provider> loadProviders(Collection<Long> providerIds) {
        if (providerIds == null || providerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Provider> providers = providerMapper.selectBatchIds(providerIds);
        if (providers == null || providers.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Provider> providerMap = new HashMap<>();
        for (Provider provider : providers) {
            providerMap.put(provider.getId(), provider);
        }
        return providerMap;
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
