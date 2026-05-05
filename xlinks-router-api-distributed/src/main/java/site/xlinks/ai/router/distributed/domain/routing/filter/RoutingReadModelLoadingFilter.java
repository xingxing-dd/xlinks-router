package site.xlinks.ai.router.distributed.domain.routing.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;

/**
 * 路由过滤链的读模型装载阶段。
 * 这里只加载后续过滤器都会使用的基础对象，避免主干流程提前混入业务细节。
 */
@Component
@Order(150)
@RequiredArgsConstructor
public class RoutingReadModelLoadingFilter implements RoutingFilter {

    private final ForwardingReadModelLoader forwardingReadModelLoader;

    @Override
    public void filter(RoutingFilterContext context) {
        context.setCustomerToken(forwardingReadModelLoader.loadCustomerToken(context.getRequest().getCustomerToken()));
        context.setCustomerAccount(forwardingReadModelLoader.loadCustomerAccount(context.getCustomerToken().getAccountId()));
        context.setModel(forwardingReadModelLoader.loadModel(context.getRequest().getModel()));

        RequestChainLogCollector.bindCustomer(
                context.getCustomerToken(),
                context.getCustomerAccount(),
                null
        );
        RequestChainLogCollector.bindModel(context.getModel());
    }
}
