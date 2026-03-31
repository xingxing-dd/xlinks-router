package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.adapter.OpenAIProviderAdapter;
import site.xlinks.ai.router.adapter.OpenAIProviderAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.OpenAIProtocol;
import site.xlinks.ai.router.dto.OpenAIProxyRequest;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ModelMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates routing, provider invocation, and usage recording for OpenAI-compatible requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIProxyService {

    private final CustomerTokenAuthService customerTokenAuthService;
    private final ProviderTokenSelectService providerTokenSelectService;
    private final OpenAIProviderAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final ModelMapper modelMapper;
    private final UsageRecordService usageRecordService;
    private final UsageEntitlementService usageEntitlementService;
    private final OpenAIUsageExtractor openAIUsageExtractor;

    public JsonNode forward(String token, String endpoint, OpenAIProxyRequest request) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, endpoint, request, requestId);
            OpenAIProviderAdapter adapter = resolveAdapter(context.getProviderType());
            JsonNode response = adapter.forward(request, context);
            UsageMetrics usageMetrics = openAIUsageExtractor.extract(response);
            usageRecordService.recordAsync(context, usageMetrics, System.currentTimeMillis() - startAt, null, null);
            return response;
        } catch (BusinessException e) {
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            throw handleUnexpectedError("OpenAI proxy request failed", context, e, startAt);
        }
    }

    public void forwardStream(String token,
                              String endpoint,
                              OpenAIProxyRequest request,
                              Consumer<OpenAIStreamEvent> onEvent) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, endpoint, request, requestId);
            OpenAIProviderAdapter adapter = resolveAdapter(context.getProviderType());
            AtomicReference<UsageMetrics> usageMetricsRef = new AtomicReference<>();
            adapter.forwardStream(request, context, event -> {
                UsageMetrics usageMetrics = openAIUsageExtractor.extract(event);
                if (usageMetrics != null) {
                    usageMetricsRef.set(usageMetrics);
                }
                onEvent.accept(event);
            });
            usageRecordService.recordAsync(context, usageMetricsRef.get(), System.currentTimeMillis() - startAt, null, null);
        } catch (BusinessException e) {
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            throw handleUnexpectedError("OpenAI proxy stream failed", context, e, startAt);
        }
    }

    public Object listModels(String token) {
        customerTokenAuthService.validateToken(token);

        List<Model> models = modelMapper.selectList(new LambdaQueryWrapper<Model>().eq(Model::getStatus, 1));
        List<Object> modelList = models.stream()
                .map(model -> Map.of(
                        "id", model.getModelCode(),
                        "object", "model",
                        "created", System.currentTimeMillis() / 1000,
                        "owned_by", "xlinks-router"
                ))
                .collect(Collectors.toList());

        return Map.of(
                "object", "list",
                "data", modelList
        );
    }

    private void validateRequest(OpenAIProxyRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求不能为空");
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型名称不能为空");
        }
        if (request.getProtocol() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的 OpenAI 协议");
        }
    }

    private ProviderInvokeContext buildContext(String token,
                                               String endpoint,
                                               OpenAIProxyRequest request,
                                               String requestId) {
        validateRequest(request);
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);

        UsageDecision usageDecision = usageEntitlementService.decide(customerToken, request.getModel());
        if (usageDecision == null || !usageDecision.isPackageEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "套餐不可用");
        }

        ModelEndpoint modelEndpoint = routeCacheService.getEndpoint(endpoint);
        if (modelEndpoint == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的 endpoint: " + endpoint);
        }

        Model model = routeCacheService.getModel(modelEndpoint.getId(), request.getModel());
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型不存在或不可用: " + request.getModel());
        }

        Provider provider = routeCacheService.getProvider(model.getProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "Provider 不可用");
        }

        ProviderToken providerToken = providerTokenSelectService.selectToken(provider.getId());

        return ProviderInvokeContext.builder()
                .requestId(requestId)
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .providerType(provider.getProviderType())
                .baseUrl(provider.getBaseUrl())
                .providerToken(providerToken.getTokenValue())
                .customerToken(token)
                .endpointCode(modelEndpoint.getEndpointCode())
                .modelId(model.getId())
                .modelCode(model.getModelCode())
                .modelName(model.getModelName())
                .inputPrice(model.getInputPrice())
                .outputPrice(model.getOutputPrice())
                .providerModel(model.getModelName())
                .planId(usageDecision.getPlanId())
                .customerTokenId(customerToken.getId())
                .accountId(customerToken.getAccountId())
                .customerModel(request.getModel())
                .build();
    }

    private OpenAIProviderAdapter resolveAdapter(String providerType) {
        OpenAIProviderAdapter adapter = adapterFactory.getAdapter(providerType);
        if (adapter == null) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "不支持的 Provider 类型: " + providerType);
        }
        return adapter;
    }

    private String buildRequestId(OpenAIProtocol protocol) {
        return protocol.getRequestIdPrefix() + UUID.randomUUID().toString().substring(0, 8);
    }

    private void recordBusinessError(ProviderInvokeContext context, BusinessException e, long startAt) {
        recordError(context, resolveHttpStatus(e.getCode()), String.valueOf(e.getCode()), e.getMessage(), startAt);
    }

    private BusinessException handleUnexpectedError(String logMessage,
                                                    ProviderInvokeContext context,
                                                    Exception e,
                                                    long startAt) {
        log.error(logMessage, e);
        recordError(context, 500, ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), startAt);
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "请求处理失败: " + e.getMessage());
    }

    private void recordError(ProviderInvokeContext context,
                             int responseStatus,
                             String errorCode,
                             String errorMessage,
                             long startAt) {
        if (context == null) {
            return;
        }
        usageRecordService.recordError(
                context,
                responseStatus,
                errorCode,
                errorMessage,
                System.currentTimeMillis() - startAt
        );
    }

    private int resolveHttpStatus(int businessCode) {
        if (businessCode == ErrorCode.UNAUTHORIZED.getCode()) {
            return 401;
        }
        if (businessCode == ErrorCode.FORBIDDEN.getCode()) {
            return 403;
        }
        if (businessCode >= 5000) {
            return 500;
        }
        return 400;
    }
}
