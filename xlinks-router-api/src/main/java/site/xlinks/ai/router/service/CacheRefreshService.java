package site.xlinks.ai.router.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.cache.CacheRefreshRequest;
import site.xlinks.ai.router.dto.cache.CacheRefreshResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Dispatches internal cache refresh requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheRefreshService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RouteCacheService routeCacheService;

    public CacheRefreshResponse refresh(CacheRefreshRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Refresh request cannot be null");
        }
        String type = normalize(request.getType());
        String action = normalize(request.getAction());

        String mode = switch (type) {
            case "model" -> {
                routeCacheService.refreshModelsOnly();
                yield "full";
            }
            case "provider" -> {
                routeCacheService.refreshProvidersOnly();
                yield "full";
            }
            case "providermodel" -> {
                routeCacheService.refreshProviderModelsOnly();
                yield "full";
            }
            case "providertoken" -> {
                routeCacheService.refreshProviderTokensOnly();
                yield "full";
            }
            case "plan" -> {
                routeCacheService.refreshPlansOnly();
                yield "full";
            }
            case "merchantroute" -> {
                routeCacheService.refreshMerchantRoutesOnly();
                yield "full";
            }
            case "customertoken" -> {
                if (request.getAccountId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "accountId is required for customerToken refresh");
                }
                routeCacheService.refreshCustomerTokensByAccountId(request.getAccountId());
                yield "incremental";
            }
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "Unsupported cache refresh type: " + request.getType());
        };

        String scope = buildScope(type, request);
        String source = normalizeBlankToUnknown(request.getSource());
        String remark = normalizeBlankToUnknown(request.getRemark());
        log.info("Cache refresh applied. source={}, type={}, action={}, mode={}, scope={}, remark={}",
                source, type, action, mode, scope, remark);
        return new CacheRefreshResponse(
                type,
                action,
                mode,
                scope,
                "cache refresh accepted",
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );
    }

    private String buildScope(String type, CacheRefreshRequest request) {
        if ("customertoken".equals(type)) {
            return "accountId=" + request.getAccountId();
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
