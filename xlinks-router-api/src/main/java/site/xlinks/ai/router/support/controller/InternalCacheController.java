package site.xlinks.ai.router.support.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.cache.CacheRefreshRequest;
import site.xlinks.ai.router.dto.cache.CacheRefreshResponse;
import site.xlinks.ai.router.support.cache.CacheRefreshService;
import site.xlinks.ai.router.support.security.InternalApiAuthService;

/**
 * 分布式模块内部缓存管理接口。
 */
@RestController
@RequestMapping("/api/internal/cache")
@RequiredArgsConstructor
public class InternalCacheController {

    private final InternalApiAuthService internalApiAuthService;
    private final CacheRefreshService cacheRefreshService;

    @PostMapping("/refresh")
    public Result<CacheRefreshResponse> refresh(@RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                                                @RequestBody CacheRefreshRequest request) {
        internalApiAuthService.validateCacheRefreshAuthorization(authorization);
        return Result.success(cacheRefreshService.refresh(request));
    }
}
