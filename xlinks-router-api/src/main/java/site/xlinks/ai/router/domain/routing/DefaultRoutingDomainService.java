package site.xlinks.ai.router.domain.routing;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.domain.routing.filter.RoutingFilter;
import site.xlinks.ai.router.domain.routing.model.RoutingExclusions;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.domain.routing.model.RoutingPlan;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

import java.util.List;

/**
 * 默认路由领域服务。
 * 负责按顺序执行过滤链，并汇总生成最终的路由计划。
 */
@Service
@RequiredArgsConstructor
public class DefaultRoutingDomainService {

    private final List<RoutingFilter> routingFilters;

    public RoutingPlan route(ForwardRequest request) {
        return route(request, RoutingExclusions.none());
    }

    public RoutingPlan route(ForwardRequest request, RoutingExclusions exclusions) {
        RoutingFilterContext filterContext = RoutingFilterContext.from(request, exclusions);
        RequestChainLogCollector.record(RequestChainLogType.DECISION_START);
        RequestChainLogCollector.record(RequestChainLogType.ROUTING_FILTER_CHAIN_START);
        for (RoutingFilter routingFilter : routingFilters) {
            routingFilter.filter(filterContext);
        }
        if (filterContext.getOrderedProviderModels() == null || filterContext.getOrderedProviderModels().isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "路由计划不完整，未找到可执行的服务商候选");
        }
        RequestChainLogCollector.record(RequestChainLogType.ROUTING_FILTER_CHAIN_COMPLETED);
        return filterContext.toPlan();
    }
}
