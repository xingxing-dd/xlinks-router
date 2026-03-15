package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.adapter.ChatProviderAdapter;
import site.xlinks.ai.router.adapter.ChatProviderAdapterFactory;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.context.RouteTarget;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.entity.CustomerModel;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.ProviderToken;

import java.util.List;
import java.util.UUID;

/**
 * Chat Service - 核心编排服务
 * 负责协调整个请求处理链路：
 * 1. 入口层：参数校验
 * 2. 鉴权层：验证客户 Token
 * 3. 权益判定层：判断使用套餐还是余额
 * 4. 模型路由层：路由到目标 Provider/Model
 * 5. Token 选择层：选择可用 Provider Token
 * 6. 适配器调用层：调用下游 Provider
 * 7. 响应转换层：转换回标准响应格式
 * 8. 记录层：记录使用情况
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final CustomerTokenAuthService customerTokenAuthService;
    private final UsageEntitlementService usageEntitlementService;
    private final ModelRouteService modelRouteService;
    private final ProviderTokenSelectService providerTokenSelectService;
    private final UsageRecordService usageRecordService;
    private final ChatProviderAdapterFactory adapterFactory;

    private final site.xlinks.ai.router.mapper.CustomerModelMapper customerModelMapper;

    /**
     * 处理 Chat Completion 请求
     *
     * @param token   客户 Token
     * @param request 请求对象
     * @return 响应对象
     */
    public ChatCompletionResponse chatCompletions(String token, ChatCompletionRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            // ========== 1. 入口层：参数校验 ==========
            validateRequest(request);

            // ========== 2. 鉴权层：验证客户 Token ==========
            CustomerToken customerToken = customerTokenAuthService.validateToken(token);

            // ========== 3. 权益判定层：判断使用套餐还是余额 ==========
            UsageDecision usageDecision = usageEntitlementService.decide(customerToken, request.getModel());

            // ========== 4. 鉴权层（额外）：检查模型权限 ==========
            if (!customerTokenAuthService.hasPermissionForModel(customerToken, request.getModel())) {
                throw new site.xlinks.ai.router.common.exception.BusinessException(
                        site.xlinks.ai.router.common.enums.ErrorCode.FORBIDDEN,
                        "无权访问该模型: " + request.getModel());
            }

            // ========== 5. 模型路由层：路由到目标 Provider/Model ==========
            RouteTarget routeTarget = modelRouteService.route(request.getModel(), usageDecision.getCurrentUsageType());

            // ========== 6. Token 选择层：选择可用 Provider Token ==========
            ProviderToken providerToken = providerTokenSelectService.selectToken(routeTarget.getProviderId());

            // ========== 7. 构建调用上下文 ==========
            ProviderInvokeContext context = ProviderInvokeContext.builder()
                    .requestId(requestId)
                    .providerId(routeTarget.getProviderId())
                    .providerCode(routeTarget.getProviderCode())
                    .providerType(routeTarget.getProviderType())
                    .baseUrl(routeTarget.getBaseUrl())
                    .providerToken(providerToken.getTokenValue())
                    .providerModel(routeTarget.getProviderModel())
                    .customerTokenId(customerToken.getId())
                    .customerModel(request.getModel())
                    .build();

            // ========== 8. 适配器调用层：调用下游 Provider ==========
            ChatProviderAdapter adapter = adapterFactory.getAdapter(routeTarget.getProviderType());
            if (adapter == null) {
                throw new site.xlinks.ai.router.common.exception.BusinessException(
                        site.xlinks.ai.router.common.enums.ErrorCode.ROUTE_ERROR,
                        "不支持的 Provider 类型: " + routeTarget.getProviderType());
            }

            ChatCompletionResponse response = adapter.chatCompletion(request, context);

            // ========== 9. 后置处理：记录使用情况 ==========
            long latencyMs = System.currentTimeMillis() - startTime;
            recordUsage(requestId, customerToken.getId(), routeTarget.getProviderId(),
                    null, providerToken.getId(), request.getModel(), response, latencyMs,
                    null, null);

            return response;

        } catch (site.xlinks.ai.router.common.exception.BusinessException e) {
            // 业务异常处理
            long latencyMs = System.currentTimeMillis() - startTime;
            handleError(requestId, token, request, startTime, e);
            throw e;
        } catch (Exception e) {
            // 未知异常处理
            long latencyMs = System.currentTimeMillis() - startTime;
            log.error("Chat completion failed", e);
            handleError(requestId, token, request, startTime, e);
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    site.xlinks.ai.router.common.enums.ErrorCode.INTERNAL_ERROR,
                    "请求处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取模型列表
     */
    public Object listModels(String token) {
        // 验证 Token
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);

        // 查询客户可用的模型列表
        // 这里简化处理：返回所有启用的模型
        // 后续可根据 customerToken 的 allowedModels 进行过滤

        List<CustomerModel> models = customerModelMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CustomerModel>()
                        .eq(CustomerModel::getStatus, 1)
        );

        List<Object> modelList = models.stream()
                .map(m -> java.util.Map.of(
                        "id", m.getLogicModelCode(),
                        "object", "model",
                        "created", System.currentTimeMillis() / 1000,
                        "owned_by", "xlinks-router"
                ))
                .collect(java.util.stream.Collectors.toList());

        return java.util.Map.of(
                "object", "list",
                "data", modelList
        );
    }

    /**
     * 校验请求参数
     */
    private void validateRequest(ChatCompletionRequest request) {
        if (request == null) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    site.xlinks.ai.router.common.enums.ErrorCode.PARAM_ERROR, "请求不能为空");
        }
        if (request.getModel() == null || request.getModel().isEmpty()) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    site.xlinks.ai.router.common.enums.ErrorCode.PARAM_ERROR, "模型名称不能为空");
        }
        if (request.getMessages() == null || request.getMessages().isEmpty()) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    site.xlinks.ai.router.common.enums.ErrorCode.PARAM_ERROR, "消息列表不能为空");
        }
    }

    /**
     * 记录使用情况
     */
    private void recordUsage(String requestId, Long customerTokenId, Long providerId,
                             Long modelId, Long providerTokenId, String requestModel,
                             ChatCompletionResponse response, long latencyMs,
                             String errorCode, String errorMessage) {
        try {
            usageRecordService.recordAsync(requestId, customerTokenId, providerId,
                    modelId, providerTokenId, requestModel, response, latencyMs,
                    errorCode, errorMessage);
        } catch (Exception e) {
            log.error("Failed to record usage", e);
        }
    }

    /**
     * 处理错误
     */
    private void handleError(String requestId, String token, ChatCompletionRequest request,
                             long startTime, Exception e) {
        try {
            CustomerToken customerToken = customerTokenAuthService.validateToken(token);
            long latencyMs = System.currentTimeMillis() - startTime;

            String errorCode = null;
            String errorMessage = null;

            if (e instanceof site.xlinks.ai.router.common.exception.BusinessException) {
                site.xlinks.ai.router.common.exception.BusinessException be =
                        (site.xlinks.ai.router.common.exception.BusinessException) e;
                errorCode = String.valueOf(be.getCode());
                errorMessage = be.getMessage();
            } else {
                errorCode = "INTERNAL_ERROR";
                errorMessage = e.getMessage();
            }

            usageRecordService.recordError(requestId, customerToken.getId(), null,
                    null, null, request.getModel(), 500, errorCode, errorMessage, latencyMs);
        } catch (Exception ex) {
            log.error("Failed to record error", ex);
        }
    }
}
