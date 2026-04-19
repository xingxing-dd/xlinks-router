package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalApiAuthServiceTest {

    @Test
    void shouldAcceptConfiguredBearerToken() {
        InternalApiAuthService service = new InternalApiAuthService();
        ReflectionTestUtils.setField(service, "internalCacheRefreshToken", "secret-token");

        assertDoesNotThrow(() -> service.validateCacheRefreshAuthorization("Bearer secret-token"));
    }

    @Test
    void shouldRejectInvalidBearerToken() {
        InternalApiAuthService service = new InternalApiAuthService();
        ReflectionTestUtils.setField(service, "internalCacheRefreshToken", "secret-token");

        assertThrows(BusinessException.class,
                () -> service.validateCacheRefreshAuthorization("Bearer wrong-token"));
    }
}
