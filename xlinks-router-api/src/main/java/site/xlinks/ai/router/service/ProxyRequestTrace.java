package site.xlinks.ai.router.service;

import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.UsageMetrics;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Request-scoped trace collector backed by ThreadLocal.
 * Collects lightweight routing/proxy milestones and emits one summary log on completion.
 */
public final class ProxyRequestTrace {

    private static final ThreadLocal<TraceContext> HOLDER = new ThreadLocal<>();
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.of("Asia/Shanghai"));
    private static final int MAX_ROUTE_EVENTS = 32;

    private ProxyRequestTrace() {
    }

    public static void begin(String requestId, ProxyRequest request) {
        TraceContext context = new TraceContext();
        context.requestId = requestId;
        context.protocol = request == null || request.getProtocol() == null ? null : request.getProtocol().getCode();
        context.stream = request != null && Boolean.TRUE.equals(request.isStream());
        context.customerModel = request == null ? null : request.getModel();
        context.requestStartAt = System.currentTimeMillis();
        HOLDER.set(context);
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void addRouteEvent(String event) {
        if (event == null || event.isBlank()) {
            return;
        }
        TraceContext context = HOLDER.get();
        if (context == null) {
            return;
        }
        if (context.routeEvents.size() >= MAX_ROUTE_EVENTS) {
            if (!context.routeEventsTruncated) {
                context.routeEvents.add("... 路由事件过多，后续内容已省略");
                context.routeEventsTruncated = true;
            }
            return;
        }
        context.routeEvents.add(event);
    }

    public static void markCustomerToken(CustomerToken customerToken) {
        TraceContext context = HOLDER.get();
        if (context == null || customerToken == null) {
            return;
        }
        context.customerTokenId = customerToken.getId();
        context.accountId = customerToken.getAccountId();
    }

    public static void markUsageDecision(UsageDecision usageDecision) {
        TraceContext context = HOLDER.get();
        if (context == null || usageDecision == null) {
            return;
        }
        context.planId = usageDecision.getPlanId();
    }

    public static void markModel(Model model) {
        TraceContext context = HOLDER.get();
        if (context == null || model == null) {
            return;
        }
        context.modelCode = model.getModelCode();
    }

    public static void markRouteResolved(Provider provider,
                                         ProviderModel providerModel,
                                         ProviderToken providerToken,
                                         ProviderPermitLease permitLease) {
        TraceContext context = HOLDER.get();
        if (context == null) {
            return;
        }
        if (provider != null) {
            context.providerId = provider.getId();
            context.providerCode = provider.getProviderCode();
        }
        if (providerToken != null) {
            context.providerTokenId = providerToken.getId();
        }
        if (providerModel != null && providerModel.getProviderModelCode() != null && !providerModel.getProviderModelCode().isBlank()) {
            context.providerModel = providerModel.getProviderModelCode();
        }
        if (permitLease != null && permitLease.runtimePolicy() != null) {
            context.requestTimeoutMs = permitLease.runtimePolicy().requestTimeoutMs();
            context.streamFirstResponseTimeoutMs = permitLease.runtimePolicy().streamFirstResponseTimeoutMs();
            context.streamIdleTimeoutMs = permitLease.runtimePolicy().streamIdleTimeoutMs();
        }
    }

    public static void markInvokeContext(ProviderInvokeContext invokeContext) {
        TraceContext context = HOLDER.get();
        if (context == null || invokeContext == null) {
            return;
        }
        context.requestId = invokeContext.getRequestId();
        context.protocol = invokeContext.getEndpointCode();
        context.accountId = invokeContext.getAccountId();
        context.customerTokenId = invokeContext.getCustomerTokenId();
        context.planId = invokeContext.getPlanId();
        context.modelCode = invokeContext.getModelCode();
        context.providerId = invokeContext.getProviderId();
        context.providerCode = invokeContext.getProviderCode();
        context.providerTokenId = invokeContext.getProviderTokenId();
        context.providerModel = invokeContext.getProviderModel();
        context.requestTimeoutMs = invokeContext.getRequestTimeoutMs();
        context.streamFirstResponseTimeoutMs = invokeContext.getStreamFirstResponseTimeoutMs();
        context.streamIdleTimeoutMs = invokeContext.getStreamIdleTimeoutMs();
    }

    public static void markFirstResponse() {
        TraceContext context = HOLDER.get();
        if (context == null || context.firstResponseAt > 0) {
            return;
        }
        context.firstResponseAt = System.currentTimeMillis();
    }

    public static void markSuccess(ProviderInvokeContext invokeContext, UsageMetrics usageMetrics) {
        TraceContext context = HOLDER.get();
        if (context == null) {
            return;
        }
        markInvokeContext(invokeContext);
        context.success = true;
        context.finishReason = "success";
        context.completedAt = System.currentTimeMillis();
        fillUsage(context, usageMetrics);
    }

    public static void markFailure(ProviderInvokeContext invokeContext,
                                   String finishReason,
                                   String errorCode,
                                   String errorMessage) {
        TraceContext context = HOLDER.get();
        if (context == null) {
            return;
        }
        markInvokeContext(invokeContext);
        context.success = false;
        context.finishReason = finishReason;
        context.errorCode = errorCode;
        context.errorMessage = errorMessage;
        context.completedAt = System.currentTimeMillis();
    }

    public static String buildSummary() {
        TraceContext context = HOLDER.get();
        if (context == null) {
            return null;
        }
        long completedAt = context.completedAt > 0 ? context.completedAt : System.currentTimeMillis();
        long elapsedMs = Math.max(completedAt - context.requestStartAt, 0L);
        Long firstResponseMs = context.firstResponseAt > 0
                ? Math.max(context.firstResponseAt - context.requestStartAt, 0L)
                : null;

        StringBuilder builder = new StringBuilder(1024);
        builder.append("代理请求汇总")
                .append('\n').append("requestId=").append(nullSafe(context.requestId))
                .append(", protocol=").append(nullSafe(context.protocol))
                .append(", stream=").append(context.stream)
                .append(", customerModel=").append(nullSafe(context.customerModel))
                .append(", modelCode=").append(nullSafe(context.modelCode))
                .append('\n').append("开始时间=").append(formatTime(context.requestStartAt))
                .append(", 首次响应时间=").append(formatTime(context.firstResponseAt))
                .append(", 完成时间=").append(formatTime(completedAt))
                .append(", 总耗时Ms=").append(elapsedMs)
                .append(", 首次响应耗时Ms=").append(firstResponseMs == null ? "-" : firstResponseMs)
                .append('\n').append("账号信息: accountId=").append(nullSafe(context.accountId))
                .append(", customerTokenId=").append(nullSafe(context.customerTokenId))
                .append(", planId=").append(nullSafe(context.planId))
                .append('\n').append("路由结果: providerId=").append(nullSafe(context.providerId))
                .append(", providerCode=").append(nullSafe(context.providerCode))
                .append(", providerTokenId=").append(nullSafe(context.providerTokenId))
                .append(", providerModel=").append(nullSafe(context.providerModel))
                .append(", requestTimeoutMs=").append(nullSafe(context.requestTimeoutMs))
                .append(", streamFirstResponseTimeoutMs=").append(nullSafe(context.streamFirstResponseTimeoutMs))
                .append(", streamIdleTimeoutMs=").append(nullSafe(context.streamIdleTimeoutMs))
                .append('\n').append("处理结果: success=").append(context.success)
                .append(", finishReason=").append(nullSafe(context.finishReason))
                .append(", errorCode=").append(nullSafe(context.errorCode))
                .append(", errorMessage=").append(nullSafe(context.errorMessage))
                .append('\n').append("用量信息: inputTokens=").append(nullSafe(context.inputTokens))
                .append(", outputTokens=").append(nullSafe(context.outputTokens))
                .append(", totalTokens=").append(nullSafe(context.totalTokens))
                .append('\n').append("路由过程:");

        if (context.routeEvents.isEmpty()) {
            builder.append('\n').append("  - 无");
        } else {
            for (int i = 0; i < context.routeEvents.size(); i++) {
                builder.append('\n')
                        .append("  ")
                        .append(i + 1)
                        .append(". ")
                        .append(context.routeEvents.get(i));
            }
        }
        return builder.toString();
    }

    private static void fillUsage(TraceContext context, UsageMetrics usageMetrics) {
        if (context == null || usageMetrics == null) {
            return;
        }
        context.inputTokens = usageMetrics.getInputTokens();
        context.outputTokens = usageMetrics.getOutputTokens();
        context.totalTokens = usageMetrics.getTotalTokens();
    }

    private static String formatTime(long epochMillis) {
        if (epochMillis <= 0) {
            return "-";
        }
        return TIME_FORMATTER.format(Instant.ofEpochMilli(epochMillis));
    }

    private static String nullSafe(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private static final class TraceContext {
        private String requestId;
        private String protocol;
        private boolean stream;
        private String customerModel;
        private Long accountId;
        private Long customerTokenId;
        private Long planId;
        private String modelCode;
        private Long providerId;
        private String providerCode;
        private Long providerTokenId;
        private String providerModel;
        private Integer requestTimeoutMs;
        private Integer streamFirstResponseTimeoutMs;
        private Integer streamIdleTimeoutMs;
        private long requestStartAt;
        private long firstResponseAt;
        private long completedAt;
        private boolean success;
        private String finishReason;
        private String errorCode;
        private String errorMessage;
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private boolean routeEventsTruncated;
        private final List<String> routeEvents = new ArrayList<>(12);
    }
}
