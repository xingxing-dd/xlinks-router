package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.cache.CacheRefreshRequest;
import site.xlinks.ai.router.dto.cache.CacheRefreshResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CacheRefreshServiceTest {

    @Test
    void shouldDispatchModelRefreshToFullRefresh() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CacheRefreshService service = new CacheRefreshService(routeCacheService);

        CacheRefreshRequest request = new CacheRefreshRequest();
        request.setType("model");
        request.setAction("updated");

        CacheRefreshResponse response = service.refresh(request);

        assertEquals("full", response.getMode());
        assertEquals("all", response.getScope());
        verify(routeCacheService, times(1)).refreshModelsOnly();
    }

    @Test
    void shouldDispatchCustomerTokenRefreshToIncrementalRefresh() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CacheRefreshService service = new CacheRefreshService(routeCacheService);

        CacheRefreshRequest request = new CacheRefreshRequest();
        request.setType("customerToken");
        request.setAction("updated");
        request.setAccountId(100L);

        CacheRefreshResponse response = service.refresh(request);

        assertEquals("incremental", response.getMode());
        assertEquals("accountId=100", response.getScope());
        verify(routeCacheService, times(1)).refreshCustomerTokensByAccountId(100L);
    }

    @Test
    void shouldRejectCustomerTokenRefreshWithoutAccountId() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CacheRefreshService service = new CacheRefreshService(routeCacheService);

        CacheRefreshRequest request = new CacheRefreshRequest();
        request.setType("customerToken");
        request.setAction("updated");

        assertThrows(BusinessException.class, () -> service.refresh(request));
    }
}
