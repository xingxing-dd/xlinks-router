package site.xlinks.ai.router.support.logging;

/**
 * 请求链路日志类型。
 * 将阶段名与日志模板集中维护，避免各处散落硬编码字符串。
 */
public enum RequestChainLogType {

    REQUEST_RECEIVED("请求进入", "开始接收客户端请求"),
    DECISION_START("路由决策", "开始执行路由决策"),
    PROTOCOL_CONTROLLER_ENTERED("接收协议请求", "协议=%s，请求已进入协议控制器"),
    PROTOCOL_REQUEST_RECEIVED("接收协议请求", "协议=%s，模型=%s，stream=%s，tokenSource=%s"),
    MODELS_REQUEST_RECEIVED("接收协议请求", "已接收 models 请求"),
    CUSTOMER_TOKEN_RESOLVED("解析客户令牌", "协议=%s，来源=%s"),
    CUSTOMER_TOKEN_VALIDATED("客户令牌校验", "客户令牌校验通过，客户=%s，令牌=%s"),
    CONSUMPTION_DECIDED("消费方式决策", "消费方式=%s，套餐=%s"),
    ROUTING_FILTER_CHAIN_START("路由过滤链", "开始执行路由过滤链"),
    FORWARD_ATTEMPT_STARTED("转发尝试", "第 %s 次尝试，服务商=%s，服务商令牌=%s，令牌掩码=%s，并发上限=%s，Permit等待=%sms，非流式超时=%sms，流式首包超时=%sms，流式空闲超时=%sms，Permit租约=%sms，Permit续租周期=%sms"),
    ROUTING_FILTER_CHAIN_COMPLETED("路由过滤链", "路由过滤链执行完成"),
    MODEL_ACCESS_ALLOWED("模型权限校验", "客户令牌和套餐均允许访问模型=%s"),
    PROVIDER_CANDIDATES_FILTERED("服务商候选筛选", "候选服务商数量=%s，商户偏好服务商ID=%s"),
    PROVIDER_SELECTED("服务商选择", "已选择服务商=%s/%s(%s)，服务商令牌=%s(%s)"),
    PERMIT_BYPASSED("并发令牌", "服务商未启用并发令牌限制，直接放行。服务商=%s，服务商令牌=%s"),
    PERMIT_ACQUIRE_INTERRUPTED("并发令牌", "并发令牌获取被中断。服务商=%s，服务商令牌=%s"),
    PERMIT_DIAGNOSTIC("并发令牌诊断", "%s，服务商=%s，服务商令牌=%s，semaphoreKey=%s，availablePermits=%s，acquiredPermits=%s，permitId=%s"),
    PERMIT_ATTEMPT_FAILED("并发令牌", "第 %s 次尝试未拿到并发令牌，服务商=%s，服务商令牌=%s，等待超时=%sms，后续动作=尝试当前服务商的下一个令牌"),
    PERMIT_ATTEMPT_ACQUIRED("并发令牌", "第 %s 次尝试获取到并发令牌，服务商=%s，服务商令牌=%s，permitId=%s"),
    SESSION_STARTED("会话开始", "已持有会话级并发令牌，准备调用上游。服务商=%s，服务商令牌=%s"),
    PERMIT_RENEW_SCHEDULED("并发令牌续约", "已启动自动续约任务。服务商=%s，服务商令牌=%s，续约间隔=%sms"),
    PERMIT_RENEWED("并发令牌续约", "并发令牌续约成功。服务商=%s，服务商令牌=%s，permitId=%s"),
    PERMIT_RENEW_FAILED("并发令牌续约", "并发令牌续约失败。服务商=%s，服务商令牌=%s，permitId=%s，原因=%s"),
    PERMIT_RELEASED("并发令牌释放", "并发令牌释放成功。服务商=%s，服务商令牌=%s，permitId=%s"),
    PERMIT_RELEASE_FAILED("并发令牌释放", "并发令牌释放失败。服务商=%s，服务商令牌=%s，permitId=%s，原因=%s"),
    UPSTREAM_DIRECT_REQUEST("调用上游", "发起非流式上游请求，服务商=%s，服务商令牌=%s，令牌掩码=%s，URL=%s"),
    UPSTREAM_STREAM_REQUEST("调用上游", "发起流式上游请求，服务商=%s，服务商令牌=%s，令牌掩码=%s，URL=%s"),
    UPSTREAM_DIRECT_RESPONSE("上游响应", "非流式上游响应状态码=%s"),
    UPSTREAM_STREAM_RESPONSE("上游响应", "流式上游响应状态码=%s"),
    UPSTREAM_DIRECT_TIMEOUT("上游超时", "非流式上游请求超时，服务商=%s"),
    UPSTREAM_STREAM_TIMEOUT("上游超时", "流式上游请求超时，服务商=%s"),
    UPSTREAM_DIRECT_ERROR("上游异常", "非流式上游转发失败，服务商=%s，原因=%s"),
    UPSTREAM_STREAM_ERROR("上游异常", "流式上游转发失败，服务商=%s，原因=%s"),
    UPSTREAM_RETRYABLE_FAILURE("上游失败", "第 %s 次尝试发生可重试失败，服务商=%s，服务商令牌=%s，失败原因=%s，后续动作=%s"),
    REQUEST_REWRITE_FALLBACK_FROM_PAYLOAD("请求改写回退", "基于解析 payload 的请求改写失败，回退原始请求体"),
    REQUEST_REWRITE_FALLBACK_FROM_RAW("请求改写回退", "基于原始请求体的改写失败，回退原始请求体"),
    STREAMING_STARTED("开始流式转发", "已进入 SSE 数据透传阶段"),
    SSE_TRANSFER_FAILED("SSE透传失败", "上游流式数据透传失败，服务商=%s，原因=%s"),
    DIRECT_RESPONSE_SUCCESS("响应完成", "上游返回成功响应，状态码=%s"),
    DIRECT_RESPONSE_ERROR("响应完成", "上游返回非成功响应，状态码=%s"),
    STREAMING_RESPONSE_SUCCESS("流式转发完成", "SSE 流已完整写出"),
    STREAMING_RESPONSE_ERROR("流式转发失败", "SSE 流写出失败，原因=%s"),
    PROTOCOL_BUSINESS_ERROR("协议异常", "协议请求业务异常，code=%s，message=%s"),
    PROTOCOL_BAD_REQUEST("协议异常", "协议请求参数错误，message=%s"),
    PROTOCOL_UNEXPECTED_ERROR("协议异常", "协议请求出现未预期异常，message=%s"),
    GLOBAL_BUSINESS_ERROR("全局异常", "业务异常，code=%s，message=%s"),
    GLOBAL_BAD_REQUEST("全局异常", "请求参数错误，message=%s"),
    GLOBAL_NO_RESOURCE("全局异常", "请求路径不存在，uri=%s"),
    GLOBAL_UNEXPECTED_ERROR("全局异常", "未处理异常，message=%s"),
    ASYNC_TIMEOUT("异步超时", "请求异步处理超时"),
    ASYNC_ERROR("异步异常", "请求异步处理出现异常");

    private final String stage;
    private final String messageTemplate;

    RequestChainLogType(String stage, String messageTemplate) {
        this.stage = stage;
        this.messageTemplate = messageTemplate;
    }

    public String stage() {
        return stage;
    }

    public String formatMessage(Object... args) {
        return String.format(messageTemplate, args);
    }
}
