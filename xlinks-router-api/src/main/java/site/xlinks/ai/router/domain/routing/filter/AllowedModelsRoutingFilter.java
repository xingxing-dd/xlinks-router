package site.xlinks.ai.router.domain.routing.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

/**
 * 模型权限过滤器。
 * 明确校验客户令牌和最终选中的套餐是否都允许访问当前模型。
 */
@Component
@Order(200)
@RequiredArgsConstructor
public class AllowedModelsRoutingFilter implements RoutingFilter {

    private final ForwardingReadModelLoader forwardingReadModelLoader;

    @Override
    public void filter(RoutingFilterContext context) {
        String modelCode = context.getModel().getModelCode();

        AllowedModelsRule customerRule = forwardingReadModelLoader.loadCustomerAllowedModelsRule(context.getCustomerToken());
        if (isModelDisallowed(customerRule, modelCode)) {
            throw new BusinessException(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST, "客户令牌不允许访问当前模型");
        }

        AllowedModelsRule planRule = forwardingReadModelLoader.loadPlanAllowedModelsRule(context.getCustomerPlan());
        if (isModelDisallowed(planRule, modelCode)) {
            throw new BusinessException(ErrorCode.MODEL_NOT_IN_ALLOWED_LIST, "当前套餐不允许访问该模型");
        }

        RequestChainLogCollector.record(RequestChainLogType.MODEL_ACCESS_ALLOWED, modelCode);
    }

    private boolean isModelDisallowed(AllowedModelsRule rule, String modelCode) {
        if (rule == null || rule.isAllowAll()) {
            return false;
        }
        if (modelCode == null || modelCode.isBlank()) {
            return true;
        }
        return rule.getAllowedModels() != null
                && !rule.getAllowedModels().isEmpty()
                && !rule.getAllowedModels().contains(modelCode.trim());
    }
}
