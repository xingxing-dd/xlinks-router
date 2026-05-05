package site.xlinks.ai.router.support.logging;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import site.xlinks.ai.router.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 ThreadLocal 的请求链路日志收集器。
 */
@Slf4j
public final class RequestChainLogCollector {

    private static final ThreadLocal<RequestChainLogSession> CURRENT = new ThreadLocal<>();
    private static final ZoneId LOG_ZONE_ID = ZoneId.systemDefault();
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private RequestChainLogCollector() {
    }

    public static RequestChainLogSession start(HttpServletRequest request, String traceId) {
        RequestChainLogSession session = new RequestChainLogSession(
                traceId,
                request == null ? null : request.getMethod(),
                request == null ? null : request.getRequestURI()
        );
        CURRENT.set(session);
        record(RequestChainLogType.REQUEST_RECEIVED);
        return session;
    }

    public static RequestChainLogSession captureCurrent() {
        return CURRENT.get();
    }

    public static void bind(RequestChainLogSession session) {
        if (session != null) {
            CURRENT.set(session);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static void record(RequestChainLogType type, Object... args) {
        if (type == null) {
            return;
        }
        recordInternal(type.stage(), type.formatMessage(args));
    }

    public static void bindForwardRequest(ForwardRequest request) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null || request == null) {
            return;
        }
        session.protocol = request.getProtocol() == null ? null : request.getProtocol().getCode();
        session.modelCode = request.getModel();
        session.stream = request.getStream();
        session.tokenSource = request.getTokenSource() == null ? null : request.getTokenSource().name();
    }

    public static void bindProtocolContext(String protocol, String tokenSource, Boolean stream) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        if (!isBlankValue(protocol)) {
            session.protocol = protocol;
        }
        if (!isBlankValue(tokenSource)) {
            session.tokenSource = tokenSource;
        }
        if (stream != null) {
            session.stream = stream;
        }
    }

    public static void bindRequestId(String requestId) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        session.requestId = requestId;
    }

    public static void bindCustomer(CustomerToken customerToken,
                                    CustomerAccount customerAccount,
                                    CustomerPlan customerPlan) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        if (customerToken != null) {
            session.customerTokenId = customerToken.getId();
            session.customerName = customerToken.getCustomerName();
            session.customerTokenName = customerToken.getTokenName();
        }
        if (customerAccount != null) {
            session.accountId = customerAccount.getId();
            session.accountName = customerAccount.getUsername();
        }
        if (customerPlan != null) {
            session.planId = customerPlan.getId();
            session.planName = customerPlan.getPlanName();
        }
    }

    public static void bindModel(Model model) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null || model == null) {
            return;
        }
        session.modelId = model.getId();
        session.modelCode = model.getModelCode();
        session.modelName = model.getModelName();
        session.modelProvider = model.getModelProvider();
    }

    public static void bindRoutingDecision(RoutingDecision decision) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null || decision == null) {
            return;
        }
        bindProvider(decision.getProvider(), decision.getProviderToken());
    }

    public static void bindProvider(Provider provider, ProviderToken providerToken) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        if (provider != null) {
            session.providerId = provider.getId();
            session.providerCode = provider.getProviderCode();
            session.providerName = provider.getProviderName();
        }
        if (providerToken != null) {
            session.providerTokenId = providerToken.getId();
            session.providerTokenName = providerToken.getTokenName();
        }
    }

    public static void bindQueryResultCount(Integer queryResultCount) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        session.queryResultCount = queryResultCount;
    }

    public static void bindQueryBalance(BigDecimal queryBalance) {
        RequestChainLogSession session = CURRENT.get();
        if (session == null) {
            return;
        }
        session.queryBalance = queryBalance;
    }

    public static void markResponseStatus(int responseStatus) {
        RequestChainLogSession session = CURRENT.get();
        if (session != null && !session.flushed.get()) {
            session.responseStatus = responseStatus;
        }
    }

    public static void markSuccess(String message) {
        RequestChainLogSession session = CURRENT.get();
        if (session != null && !session.flushed.get() && session.level == LogLevel.NONE) {
            session.resultMessage = message;
        }
    }

    public static void markBusinessFailure(String message) {
        RequestChainLogSession session = CURRENT.get();
        if (session != null && !session.flushed.get() && session.level.ordinal() < LogLevel.WARN.ordinal()) {
            session.level = LogLevel.WARN;
            session.resultMessage = message;
        }
    }

    public static void markUnexpectedFailure(String message, Throwable throwable) {
        RequestChainLogSession session = CURRENT.get();
        if (session != null && !session.flushed.get()) {
            session.level = LogLevel.ERROR;
            session.resultMessage = message;
            session.throwable = throwable;
        }
    }

    public static void flush(RequestChainLogSession session) {
        if (session == null || !session.flushed.compareAndSet(false, true)) {
            return;
        }

        if (session.level == LogLevel.NONE) {
            if (session.responseStatus != null && session.responseStatus >= 400) {
                session.level = LogLevel.WARN;
                if (session.resultMessage == null) {
                    session.resultMessage = "请求处理失败";
                }
            } else if (session.resultMessage == null) {
                session.resultMessage = "请求处理成功";
            }
        }

        String rendered = session.render();
        if (session.level == LogLevel.ERROR) {
            if (session.throwable != null) {
                log.error(rendered, session.throwable);
            } else {
                log.error(rendered);
            }
            return;
        }
        if (session.level == LogLevel.WARN) {
            log.warn(rendered);
            return;
        }
        log.info(rendered);
    }

    private static void recordInternal(String stage, String message) {
        RequestChainLogSession session = CURRENT.get();
        if (session != null) {
            session.addEvent(stage, message);
        }
    }

    private static boolean isBlankValue(String value) {
        return value == null || value.isBlank();
    }

    public enum LogLevel {
        NONE,
        WARN,
        ERROR
    }

    public static final class RequestChainLogSession {

        private final long startedAtNano = System.nanoTime();
        private final long startedAtEpochMs = System.currentTimeMillis();
        private final String traceId;
        private final String method;
        private final String uri;
        private final List<RequestChainLogEvent> events = new ArrayList<>();
        private final AtomicBoolean flushed = new AtomicBoolean(false);

        private String requestId;
        private String protocol;
        private String tokenSource;
        private Boolean stream;
        private Long accountId;
        private String accountName;
        private Long customerTokenId;
        private String customerName;
        private String customerTokenName;
        private Long planId;
        private String planName;
        private Long modelId;
        private String modelCode;
        private String modelName;
        private String modelProvider;
        private Long providerId;
        private String providerCode;
        private String providerName;
        private Long providerTokenId;
        private String providerTokenName;
        private Integer queryResultCount;
        private BigDecimal queryBalance;
        private Integer responseStatus;
        private LogLevel level = LogLevel.NONE;
        private String resultMessage;
        private Throwable throwable;

        private RequestChainLogSession(String traceId, String method, String uri) {
            this.traceId = traceId;
            this.method = method;
            this.uri = uri;
        }

        private synchronized void addEvent(String stage, String message) {
            if (flushed.get()) {
                return;
            }
            events.add(new RequestChainLogEvent(elapsedMs(), stage, message));
        }

        private long elapsedMs() {
            return Math.max((System.nanoTime() - startedAtNano) / 1_000_000L, 0L);
        }

        private synchronized String render() {
            if (isModelsQueryRequest()) {
                return renderModelsQueryLog();
            }
            if (isBalanceQueryRequest()) {
                return renderBalanceQueryLogSafe();
            }
            return renderForwardingLog();
        }

        private String renderForwardingLog() {
            StringBuilder builder = new StringBuilder();
            builder.append("请求链路日志")
                    .append(" | 结果=").append(resultMessage == null ? "请求处理完成" : resultMessage)
                    .append(" | 总耗时=").append(elapsedMs()).append("ms")
                    .append(" | HTTP状态=").append(responseStatus == null ? "-" : responseStatus)
                    .append(" | traceId=").append(defaultValue(traceId))
                    .append(" | requestId=").append(defaultValue(requestId))
                    .append('\n');

            builder.append("请求摘要")
                    .append(" | method=").append(defaultValue(method))
                    .append(" | uri=").append(defaultValue(uri))
                    .append(" | protocol=").append(defaultValue(protocol))
                    .append(" | stream=").append(stream == null ? "-" : stream)
                    .append(" | tokenSource=").append(defaultValue(tokenSource))
                    .append('\n');

            builder.append("业务摘要")
                    .append(" | 客户账号=").append(formatNameAndId(accountName, null, accountId))
                    .append(" | 客户令牌=").append(formatNameAndId(joinNames(customerName, customerTokenName), null, customerTokenId))
                    .append(" | 套餐=").append(formatNameAndId(planName, null, planId))
                    .append(" | 模型=").append(formatNameAndId(modelName, modelCode, modelId))
                    .append(" | 模型厂商=").append(defaultValue(modelProvider))
                    .append(" | 服务商=").append(formatNameAndId(providerName, providerCode, providerId))
                    .append(" | 服务商令牌=").append(formatNameAndId(providerTokenName, null, providerTokenId))
                    .append('\n');

            appendEvents(builder);
            return builder.toString();
        }

        private String renderModelsQueryLog() {
            StringBuilder builder = new StringBuilder();
            builder.append("模型列表查询日志")
                    .append(" | 结果=").append(resultMessage == null ? "请求处理成功" : resultMessage)
                    .append(" | 总耗时=").append(elapsedMs()).append("ms")
                    .append(" | HTTP状态=").append(responseStatus == null ? "-" : responseStatus)
                    .append(" | traceId=").append(defaultValue(traceId))
                    .append('\n');

            builder.append("请求摘要")
                    .append(" | method=").append(defaultValue(method))
                    .append(" | uri=").append(defaultValue(uri))
                    .append(" | protocol=").append(defaultValue(protocol))
                    .append(" | tokenSource=").append(defaultValue(tokenSource))
                    .append('\n');

            builder.append("查询摘要")
                    .append(" | 客户账号=").append(formatNameAndId(accountName, null, accountId))
                    .append(" | 客户令牌=").append(formatNameAndId(joinNames(customerName, customerTokenName), null, customerTokenId))
                    .append(" | 返回模型数=").append(queryResultCount == null ? "-" : queryResultCount)
                    .append('\n');

            appendEvents(builder);
            return builder.toString();
        }

        private String renderBalanceQueryLog() {
            StringBuilder builder = new StringBuilder();
            builder.append("浣欓鏌ヨ鏃ュ織")
                    .append(" | 缁撴灉=").append(resultMessage == null ? "璇锋眰澶勭悊鎴愬姛" : resultMessage)
                    .append(" | 鎬昏€楁椂=").append(elapsedMs()).append("ms")
                    .append(" | HTTP鐘舵€?").append(responseStatus == null ? "-" : responseStatus)
                    .append(" | traceId=").append(defaultValue(traceId))
                    .append('\n');

            builder.append("璇锋眰鎽樿")
                    .append(" | method=").append(defaultValue(method))
                    .append(" | uri=").append(defaultValue(uri))
                    .append(" | protocol=").append(defaultValue(protocol))
                    .append(" | tokenSource=").append(defaultValue(tokenSource))
                    .append('\n');

            builder.append("鏌ヨ鎽樿")
                    .append(" | 瀹㈡埛璐﹀彿=").append(formatNameAndId(accountName, null, accountId))
                    .append(" | 瀹㈡埛浠ょ墝=").append(formatNameAndId(joinNames(customerName, customerTokenName), null, customerTokenId))
                    .append(" | 鍙敤浣欓=").append(queryBalance == null ? "-" : queryBalance.toPlainString())
                    .append(" USD")
                    .append('\n');

            appendEvents(builder);
            return builder.toString();
        }

        private String renderBalanceQueryLogSafe() {
            StringBuilder builder = new StringBuilder();
            builder.append("Balance Query Log")
                    .append(" | result=").append(resultMessage == null ? "Request completed successfully" : resultMessage)
                    .append(" | elapsedMs=").append(elapsedMs()).append("ms")
                    .append(" | httpStatus=").append(responseStatus == null ? "-" : responseStatus)
                    .append(" | traceId=").append(defaultValue(traceId))
                    .append('\n');

            builder.append("Request Summary")
                    .append(" | method=").append(defaultValue(method))
                    .append(" | uri=").append(defaultValue(uri))
                    .append(" | protocol=").append(defaultValue(protocol))
                    .append(" | tokenSource=").append(defaultValue(tokenSource))
                    .append('\n');

            builder.append("Query Summary")
                    .append(" | customerAccount=").append(formatNameAndId(accountName, null, accountId))
                    .append(" | customerToken=").append(formatNameAndId(joinNames(customerName, customerTokenName), null, customerTokenId))
                    .append(" | availableBalance=").append(queryBalance == null ? "-" : queryBalance.toPlainString())
                    .append(" USD")
                    .append('\n');

            appendEvents(builder);
            return builder.toString();
        }

        private void appendEvents(StringBuilder builder) {
            builder.append("关键节点:");
            int lastEventIndex = events.size() - 1;
            for (int index = 0; index < events.size(); index++) {
                RequestChainLogEvent event = events.get(index);
                builder.append('\n')
                        .append(" - [").append(event.elapsedMs()).append("ms]")
                        .append('[').append(defaultValue(event.stage())).append(']')
                        .append(' ').append(renderEventMessage(event, index, lastEventIndex));
            }
        }

        private boolean isModelsQueryRequest() {
            return "models".equals(protocol) || "/v1/models".equals(uri);
        }

        private boolean isBalanceQueryRequest() {
            return "balance".equals(protocol) || "/user/balance".equals(uri);
        }

        private String renderEventMessage(RequestChainLogEvent event, int index, int lastEventIndex) {
            StringBuilder messageBuilder = new StringBuilder(defaultValue(event.message()));
            if (index == 0) {
                messageBuilder.append(" | 开始时间=").append(formatTime(startedAtEpochMs));
            }
            if (index == lastEventIndex) {
                messageBuilder.append(" | 结束时间=").append(formatTime(System.currentTimeMillis()));
            }
            return messageBuilder.toString();
        }

        private String formatNameAndId(String name, String code, Long id) {
            String label = defaultValue(name);
            if (!isBlank(code)) {
                label = label.equals("-") ? code : label + "/" + code;
            }
            if (id == null) {
                return label;
            }
            return label + "(" + id + ")";
        }

        private String joinNames(String left, String right) {
            if (isBlank(left)) {
                return defaultValue(right);
            }
            if (isBlank(right)) {
                return defaultValue(left);
            }
            return left + "/" + right;
        }

        private String defaultValue(String value) {
            return isBlank(value) ? "-" : value;
        }

        private boolean isBlank(String value) {
            return value == null || value.isBlank();
        }

        private String formatTime(long epochMs) {
            return LOG_TIME_FORMATTER.format(Instant.ofEpochMilli(epochMs).atZone(LOG_ZONE_ID));
        }
    }

    private record RequestChainLogEvent(long elapsedMs, String stage, String message) {
    }
}
