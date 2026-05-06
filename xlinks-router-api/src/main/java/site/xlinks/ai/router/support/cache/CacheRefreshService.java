package site.xlinks.ai.router.support.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.cache.CacheRefreshRequest;
import site.xlinks.ai.router.dto.cache.CacheRefreshResponse;
import site.xlinks.ai.router.infrastructure.cache.RoutingSnapshotCacheService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheRefreshService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    public CacheRefreshResponse refresh(CacheRefreshRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "\u5237\u65b0\u8bf7\u6c42\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String type = normalize(request.getType());
        String action = normalize(request.getAction());

        switch (type) {
            case "", "all" -> routingSnapshotCacheService.refreshAll();
            case "model" -> routingSnapshotCacheService.refreshAll();
            case "provider", "providermodel", "providertoken", "plan", "merchantroute" -> routingSnapshotCacheService.refreshAll();
            case "customertoken" -> {
                if (request.getAccountId() != null) {
                    routingSnapshotCacheService.refreshCustomerTokenByAccountId(request.getAccountId());
                } else if (request.getId() != null) {
                    routingSnapshotCacheService.refreshCustomerTokenById(request.getId());
                } else {
                    routingSnapshotCacheService.refreshAll();
                }
            }
            case "wallet" -> {
                if (request.getAccountId() != null) {
                    routingSnapshotCacheService.refreshWalletByAccountId(request.getAccountId());
                } else {
                    routingSnapshotCacheService.refreshAll();
                }
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "\u4e0d\u652f\u6301\u7684\u7f13\u5b58\u5237\u65b0\u7c7b\u578b: " + request.getType());
        }

        String scope = buildScope(type, request);
        log.debug("Local route snapshot refresh handled. source={} type={} action={} scope={} remark={}",
                normalizeBlankToUnknown(request.getSource()), type, action, scope, normalizeBlankToUnknown(request.getRemark()));
        return new CacheRefreshResponse(
                type,
                action,
                "local-snapshot",
                scope,
                "\u7f13\u5b58\u5237\u65b0\u5df2\u53d7\u7406",
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }

    private String buildScope(String type, CacheRefreshRequest request) {
        if ("customertoken".equals(type) || "wallet".equals(type)) {
            return request.getAccountId() == null ? "all" : "accountId=" + request.getAccountId();
        }
        if (request.getId() != null) {
            return "id=" + request.getId();
        }
        if (request.getProviderId() != null) {
            return "providerId=" + request.getProviderId();
        }
        if (request.getModelId() != null) {
            return "modelId=" + request.getModelId();
        }
        if (request.getPlanId() != null) {
            return "planId=" + request.getPlanId();
        }
        return "all";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeBlankToUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
