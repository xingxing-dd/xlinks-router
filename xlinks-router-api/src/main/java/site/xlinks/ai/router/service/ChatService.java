package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.adapter.ChatProviderAdapter;
import site.xlinks.ai.router.adapter.ChatProviderAdapterFactory;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.ModelEndpoint;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ModelMapper;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

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
    private final ProviderTokenSelectService providerTokenSelectService;
    private final ChatProviderAdapterFactory adapterFactory;
    private final RouteCacheService routeCacheService;
    private final ModelMapper modelMapper;
    private final UsageRecordService usageRecordService;
    private final UsageEntitlementService usageEntitlementService;
    /**
     * 处理 Chat Completion 请求
     *
     * @param token   客户 Token
     * @param request 请求对象
     * @return 响应对象
     */
    public ChatCompletionResponse chatCompletions(String token, String endpoint, ChatCompletionRequest request) {
        String requestId = "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8);
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, endpoint, request, requestId);
            String providerType = context.getProviderType();
            ChatProviderAdapter adapter1 = adapterFactory.getAdapter(providerType);
            if (adapter1 == null) {
                throw new site.xlinks.ai.router.common.exception.BusinessException(
                        ErrorCode.ROUTE_ERROR,
                        "不支持的 Provider 类型: " + providerType);
            }
            ChatProviderAdapter adapter = adapter1;
            ChatCompletionResponse response = adapter.chatCompletion(request, context);
            usageRecordService.recordAsync(context, response, System.currentTimeMillis() - startAt, null, null);
            return response;
        } catch (BusinessException e) {
            if (context != null) {
                usageRecordService.recordError(context, e.getCode(), String.valueOf(e.getCode()), e.getMessage(), System.currentTimeMillis() - startAt);
            }
            throw e;
        } catch (Exception e) {
            log.error("Chat completion failed", e);
            if (context != null) {
                usageRecordService.recordError(context, 500, ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), System.currentTimeMillis() - startAt);
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "请求处理失败: " + e.getMessage());
        }
    }

    public void chatCompletionsStream(String token,
                                      String endpoint,
                                      ChatCompletionRequest request,
                                      Consumer<String> onEvent) {
        String requestId = "chatcmpl-" + UUID.randomUUID().toString().substring(0, 8);
        long startAt = System.currentTimeMillis();
        ProviderInvokeContext context = null;
        try {
            context = buildContext(token, endpoint, request, requestId);
            String providerType = context.getProviderType();
            ChatProviderAdapter adapter1 = adapterFactory.getAdapter(providerType);
            if (adapter1 == null) {
                throw new site.xlinks.ai.router.common.exception.BusinessException(
                        ErrorCode.ROUTE_ERROR,
                        "不支持的 Provider 类型: " + providerType);
            }
            ChatProviderAdapter adapter = adapter1;
            // 直接传递 onEvent，适配器会处理 SSE 格式
            adapter.chatCompletionStream(request, context, onEvent);
        } catch (BusinessException e) {
            if (context != null) {
                usageRecordService.recordError(context, e.getCode(), String.valueOf(e.getCode()), e.getMessage(), System.currentTimeMillis() - startAt);
            }
            throw e;
        } catch (Exception e) {
            log.error("Chat completion stream failed", e);
            if (context != null) {
                usageRecordService.recordError(context, 500, ErrorCode.INTERNAL_ERROR.name(), e.getMessage(), System.currentTimeMillis() - startAt);
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "请求处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取模型列表
     */
    public Object listModels(String token) {
        // 验证 Token
        customerTokenAuthService.validateToken(token);

        // 查询所有启用的模型
        List<Model> models = modelMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Model>()
                        .eq(Model::getStatus, 1)
        );

        List<Object> modelList = models.stream()
                .map(m -> java.util.Map.of(
                        "id", m.getModelCode(),
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

    private ProviderInvokeContext buildContext(String token,
                                               String endpoint,
                                               ChatCompletionRequest request,
                                               String requestId) {
        validateRequest(request);
        CustomerToken customerToken = customerTokenAuthService.validateToken(token);

        site.xlinks.ai.router.context.UsageDecision usageDecision =
                usageEntitlementService.decide(customerToken, request.getModel());
        if (usageDecision == null || Boolean.FALSE.equals(usageDecision.isPackageEnabled())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "套餐不可用");
        }

        ModelEndpoint modelEndpoint = routeCacheService.getEndpoint(endpoint);
        if (modelEndpoint == null) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "无效的 endpoint: " + endpoint);
        }

        Model model = routeCacheService.getModel(modelEndpoint.getId(), request.getModel());
        if (model == null) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    ErrorCode.PARAM_ERROR,
                    "模型不存在或不可用: " + request.getModel());
        }

        Provider provider = routeCacheService.getProvider(model.getProviderId());
        if (provider == null || provider.getStatus() == null || provider.getStatus() != 1) {
            throw new site.xlinks.ai.router.common.exception.BusinessException(
                    ErrorCode.ROUTE_ERROR,
                    "Provider 不可用");
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
}
