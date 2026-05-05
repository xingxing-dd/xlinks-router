package site.xlinks.ai.router.distributed.support.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingReadModelLoader;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogType;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserBalanceQueryService {

    private static final String QUERY_PROTOCOL = "balance";
    private static final String CURRENCY_UNIT = "USD";

    private final CustomerTokenResolver customerTokenResolver;
    private final ForwardingReadModelLoader forwardingReadModelLoader;

    public Map<String, Object> queryBalance(HttpServletRequest request) {
        CustomerTokenResolver.ResolvedCustomerToken resolvedCustomerToken =
                customerTokenResolver.resolveOpenAiToken(request);
        RequestChainLogCollector.bindProtocolContext(
                QUERY_PROTOCOL,
                resolvedCustomerToken.source() == null ? null : resolvedCustomerToken.source().name(),
                Boolean.FALSE
        );
        RequestChainLogCollector.record(
                RequestChainLogType.CUSTOMER_TOKEN_RESOLVED,
                QUERY_PROTOCOL,
                resolvedCustomerToken.source()
        );

        CustomerToken cachedToken = forwardingReadModelLoader.loadCustomerToken(resolvedCustomerToken.value());
        CustomerToken customerToken = forwardingReadModelLoader.loadFreshCustomerToken(cachedToken, resolvedCustomerToken.value());
        validateCustomerToken(customerToken);

        CustomerAccount customerAccount = forwardingReadModelLoader.loadCustomerAccount(customerToken.getAccountId());
        RequestChainLogCollector.bindCustomer(customerToken, customerAccount, null);

        BigDecimal totalBalance = resolveWalletBalance(customerToken.getAccountId())
                .add(resolveAvailablePlanBalance(customerToken.getAccountId(), LocalDateTime.now()));
        RequestChainLogCollector.bindQueryBalance(totalBalance);
        RequestChainLogCollector.markSuccess("Balance query succeeded");

        return Map.of(
                "is_active", Boolean.TRUE,
                "balance", totalBalance,
                "unit", CURRENCY_UNIT
        );
    }

    private void validateCustomerToken(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Customer token disabled");
        }
        if (customerToken.getExpireTime() != null && LocalDateTime.now().isAfter(customerToken.getExpireTime())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Customer token expired");
        }
    }

    private BigDecimal resolveWalletBalance(Long accountId) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }
        CustomerMainWallet wallet = forwardingReadModelLoader.loadCustomerMainWallet(accountId);
        if (wallet == null) {
            return BigDecimal.ZERO;
        }
        if (wallet.getStatus() == null || wallet.getStatus() != 1) {
            return BigDecimal.ZERO;
        }
        if (wallet.getAllowOut() == null || wallet.getAllowOut() != 1) {
            return BigDecimal.ZERO;
        }
        return defaultDecimal(wallet.getAvailableBalance());
    }

    private BigDecimal resolveAvailablePlanBalance(Long accountId, LocalDateTime now) {
        if (accountId == null) {
            return BigDecimal.ZERO;
        }
        List<CustomerPlan> availablePlans = forwardingReadModelLoader.loadAvailablePlans(accountId);
        if (availablePlans == null || availablePlans.isEmpty()) {
            return BigDecimal.ZERO;
        }

        LocalDate today = now.toLocalDate();
        BigDecimal total = BigDecimal.ZERO;
        for (CustomerPlan availablePlan : availablePlans) {
            total = total.add(resolvePlanRemainingQuota(availablePlan, now, today));
        }
        return total;
    }

    private BigDecimal resolvePlanRemainingQuota(CustomerPlan plan, LocalDateTime now, LocalDate today) {
        if (!isPlanCurrentlyUsable(plan, now, today)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalRemaining = positive(defaultDecimal(plan.getTotalQuota())
                .subtract(defaultDecimal(plan.getTotalUsedQuota())));
        BigDecimal dailyRemaining = resolveDailyRemainingQuota(plan, today);
        return totalRemaining.min(dailyRemaining);
    }

    private BigDecimal resolveDailyRemainingQuota(CustomerPlan plan, LocalDate today) {
        BigDecimal dailyQuota = defaultDecimal(plan.getDailyQuota());
        if (dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        boolean refreshedToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);
        if (!refreshedToday) {
            return dailyQuota;
        }
        return positive(dailyQuota.subtract(defaultDecimal(plan.getUsedQuota())));
    }

    private boolean isPlanCurrentlyUsable(CustomerPlan plan, LocalDateTime now, LocalDate today) {
        if (plan == null || plan.getStatus() == null || plan.getStatus() != 1) {
            return false;
        }
        if (plan.getPlanExpireTime() != null && !plan.getPlanExpireTime().isAfter(now)) {
            return false;
        }

        BigDecimal totalQuota = defaultDecimal(plan.getTotalQuota());
        if (totalQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (defaultDecimal(plan.getTotalUsedQuota()).compareTo(totalQuota) >= 0) {
            return false;
        }

        BigDecimal dailyQuota = defaultDecimal(plan.getDailyQuota());
        if (dailyQuota.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        LocalDateTime refreshTime = plan.getQuotaRefreshTime();
        boolean refreshedToday = refreshTime != null && refreshTime.toLocalDate().isEqual(today);
        return !refreshedToday || defaultDecimal(plan.getUsedQuota()).compareTo(dailyQuota) < 0;
    }

    private BigDecimal positive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
