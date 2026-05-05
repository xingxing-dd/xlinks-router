package site.xlinks.ai.router.domain.routing.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.domain.routing.model.RoutingFilterContext;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户令牌校验过滤器。
 * 这里只基于本地快照做状态、过期和额度校验。
 */
@Component
@Order(160)
@RequiredArgsConstructor
public class CustomerTokenValidationRoutingFilter implements RoutingFilter {

    @Override
    public void filter(RoutingFilterContext context) {
        CustomerToken customerToken = context.getCustomerToken();
        assertTokenState(customerToken);
        assertQuotaAvailable(customerToken);

        RequestChainLogCollector.bindCustomer(customerToken, context.getCustomerAccount(), context.getCustomerPlan());
        RequestChainLogCollector.record(
                RequestChainLogType.CUSTOMER_TOKEN_VALIDATED,
                valueOrDash(customerToken.getCustomerName()),
                valueOrDash(customerToken.getTokenName())
        );
    }

    private void assertTokenState(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "客户令牌已禁用");
        }
        if (customerToken.getExpireTime() != null && LocalDateTime.now().isAfter(customerToken.getExpireTime())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "客户令牌已过期");
        }
    }

    private void assertQuotaAvailable(CustomerToken customerToken) {
        BigDecimal totalQuota = customerToken.getTotalQuota();
        if (totalQuota != null && totalQuota.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalUsedQuota = defaultDecimal(customerToken.getTotalUsedQuota());
            if (totalUsedQuota.compareTo(totalQuota) >= 0) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "客户令牌总额度已耗尽");
            }
        }

        BigDecimal dailyQuota = customerToken.getDailyQuota();
        if (dailyQuota == null || dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal dailyUsedQuota = defaultDecimal(customerToken.getUsedQuota());
        if (dailyUsedQuota.compareTo(dailyQuota) >= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "客户令牌当日额度已耗尽");
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
