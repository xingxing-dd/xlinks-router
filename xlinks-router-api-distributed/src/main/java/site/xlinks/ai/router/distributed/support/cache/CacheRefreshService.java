package site.xlinks.ai.router.distributed.support.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.dto.cache.CacheRefreshRequest;
import site.xlinks.ai.router.distributed.dto.cache.CacheRefreshResponse;
import site.xlinks.ai.router.distributed.infrastructure.cache.RoutingSnapshotCacheService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 内部缓存刷新分发服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheRefreshService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    public CacheRefreshResponse refresh(CacheRefreshRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "刷新请求不能为空");
        }
        String type = normalize(request.getType());
        String action = normalize(request.getAction());

        switch (type) {
            case "", "all" -> routingSnapshotCacheService.refreshAll();
            case "model" -> {
                if (request.getModelId() == null && request.getId() == null) {
                    routingSnapshotCacheService.refreshAll();
                } else {
                    routingSnapshotCacheService.refreshAll();
                }
            }
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
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的缓存刷新类型: " + request.getType());
        }

        String scope = buildScope(type, request);
        log.info("已执行本地路由快照刷新。source={} type={} action={} scope={} remark={}",
                normalizeBlankToUnknown(request.getSource()), type, action, scope, normalizeBlankToUnknown(request.getRemark()));
        return new CacheRefreshResponse(
                type,
                action,
                "local-snapshot",
                scope,
                "缓存刷新已受理",
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
