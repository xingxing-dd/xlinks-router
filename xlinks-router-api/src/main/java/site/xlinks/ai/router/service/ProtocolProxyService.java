package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapter;
import site.xlinks.ai.router.adapter.ProviderProtocolAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Orchestrates routing, provider invocation, and usage recording.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProtocolProxyService {

    private final CustomerTokenAuthService customerTokenAuthService;
    private final ProviderTokenSelectService providerTokenSelectService;
    private final ProviderProtocolAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final UsageRecordService usageRecordService;
    private final UsageEntitlementService usageEntitlementService;
    private final UsageExtractor usageExtractor;

    public JsonNode forward(String token, ProxyRequest request) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, request, requestId);
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            JsonNode response = adapter.forward(request, context);
            UsageMetrics usageMetrics = usageExtractor.extract(response, context.getCacheHitStrategy());
            usageRecordService.recordAsync(context, usageMetrics, System.currentTimeMillis() - startAt, null, null);
            return response;
        } catch (BusinessException e) {
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            throw handleUnexpectedError("Proxy request failed", context, e, startAt);
        }
    }

    public void forwardStream(String token,
                              ProxyRequest request,
                              Consumer<StreamEvent> onEvent) {
        String requestId = buildRequestId(request.getProtocol());
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, request, requestId);
            ProviderProtocolAdapter adapter = resolveAdapter(request.getProtocol());
            AtomicReference<UsageMetrics> usageMetricsRef = new AtomicReference<>();
            AtomicReference<Integer> responseMsRef = new AtomicReference<>();
            String cacheHitStrategy = context.getCacheHitStrategy();
            adapter.forwardStream(request, context, event -> {
                if (isFirstResponseDataEvent(event)) {
                    long firstResponseMs = System.currentTimeMillis() - startAt;
                    int normalized = firstResponseMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(firstResponseMs, 0L);
                    responseMsRef.compareAndSet(null, normalized);
                }
                UsageMetrics usageMetrics = usageExtractor.extract(event, cacheHitStrategy);
                if (usageMetrics != null) {
                    usageMetricsRef.set(usageMetrics);
                }
                onEvent.accept(event);
            });
            usageRecordService.recordAsync(
                    context,
                    usageMetricsRef.get(),
                    System.currentTimeMillis() - startAt,
                    responseMsRef.get(),
                    null,
                    null
            );
        } catch (BusinessException e) {
            recordBusinessError(context, e, startAt);
            throw e;
        } catch (Exception e) {
            throw handleUnexpectedError("Proxy stream failed", context, e, startAt);
        }
    }

    public Object listModels(String token) {
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);
        long createdEpoch = System.currentTimeMillis() / 1000;
        List<Model> models = routeCacheService.listModels();
        List<Object> modelList = models.stream()
                .filter(model -> model.getModelCode() != null && !model.getModelCode().isBlank())
                .filter(model -> customerTokenAuthService.hasPermissionForModel(customerToken, model.getModelCode()))
                .map(model -> Map.of(
                        "id", model.getModelCode(),
                        "object", "model",
                        "created", createdEpoch,
                        "owned_by", "xlinks-router"
                ))
                .collect(Collectors.toList());

        return Map.of(
                "object", "list",
                "data", modelList
        );
    }

    private void validateRequest(ProxyRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Request must not be null");
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model must not be blank");
        }
        if (request.getProtocol() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Unsupported protocol");
        }
    }

    private ProviderInvokeContext buildContext(String token,
                                               ProxyRequest request,
                                               String requestId) {
        validateRequest(request);
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);

        UsageDecision usageDecision = usageEntitlementService.decide(customerToken, request.getModel());
        if (usageDecision == null || !usageDecision.isPackageEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Customer plan is unavailable");
        }

        String endpointCode = request.getProtocol().getCode();
        Model model = routeCacheService.getModel(request.getModel());
        if (model == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Model does not exist or is unavailable: " + request.getModel());
        }

        List<ProviderModel> providerModels = routeCacheService.listProviderModelsByPriority(model.getId(), request.getProtocol());
        if (providerModels == null || providerModels.isEmpty()) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR,
                    "No available provider mapping for model and protocol: " + request.getModel());
        }

        Provider provider = null;
        ProviderModel providerModel = null;
        ProviderToken providerToken = null;
        for (ProviderModel candidate : providerModels) {
            if (candidate == null || candidate.getProviderId() == null) {
                continue;
            }
            Provider candidateProvider = routeCacheService.getProvider(candidate.getProviderId());
            if (candidateProvider == null || candidateProvider.getStatus() == null || candidateProvider.getStatus() != 1) {
                continue;
            }
            ProviderToken candidateToken = providerTokenSelectService.selectTokenOrNull(candidateProvider.getId());
            if (candidateToken == null) {
                continue;
            }
            provider = candidateProvider;
            providerModel = candidate;
            providerToken = candidateToken;
            break;
        }

        if (provider == null || providerModel == null || providerToken == null) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR,
                    "No available provider token for model and protocol: " + request.getModel());
        }

        String upstreamModelCode = providerModel.getProviderModelCode();
        if (upstreamModelCode == null || upstreamModelCode.isBlank()) {
            upstreamModelCode = model.getModelCode();
        }

        return ProviderInvokeContext.builder()
                .requestId(requestId)
                .providerId(provider.getId())
                .providerCode(provider.getProviderCode())
                .providerName(provider.getProviderName())
                .baseUrl(provider.getBaseUrl())
                .providerToken(providerToken.getTokenValue())
                .customerToken(token)
                .endpointCode(endpointCode)
                .modelId(model.getId())
                .modelCode(model.getModelCode())
                .modelName(model.getModelName())
                .inputPrice(model.getInputPrice())
                .cacheHitPrice(model.getCacheHitPrice())
                .outputPrice(model.getOutputPrice())
                .cacheHitStrategy(provider.getCacheHitStrategy())
                .providerModel(upstreamModelCode)
                .planId(usageDecision.getPlanId())
                .customerTokenId(customerToken.getId())
                .accountId(customerToken.getAccountId())
                .customerModel(request.getModel())
                .build();
    }

    private ProviderProtocolAdapter resolveAdapter(ProxyProtocol protocol) {
        ProviderProtocolAdapter adapter = adapterFactory.getAdapter(protocol);
        if (adapter == null) {
            throw new BusinessException(ErrorCode.ROUTE_ERROR, "Unsupported protocol: " + protocol);
        }
        return adapter;
    }

    private String buildRequestId(ProxyProtocol protocol) {
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
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "Request processing failed: " + e.getMessage());
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

    private boolean isFirstResponseDataEvent(StreamEvent event) {
        if (event == null || event.isDoneSignal()) {
            return false;
        }
        if (event.hasData()) {
            String data = event.joinedData();
            return data != null && !data.isBlank();
        }
        if (event.getEvent() != null && !event.getEvent().isBlank()) {
            return true;
        }
        return event.getComments() != null && !event.getComments().isEmpty();
    }
}


