package site.xlinks.ai.router.domain.routing.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

/**
 * 路由请求基础参数校验过滤器。
 */
@Component
@Order(100)
public class RoutingContextValidationFilter implements RoutingFilter {

    @Override
    public void filter(RoutingFilterContext context) {
        ForwardRequest request = context.getRequest();
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "路由请求不能为空");
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "模型不能为空");
        }
        if (request.getProtocol() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "协议不能为空");
        }
        if (request.getCustomerToken() == null || request.getCustomerToken().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "客户令牌不能为空");
        }
    }
}
