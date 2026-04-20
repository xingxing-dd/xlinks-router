package site.xlinks.ai.router.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ProviderMapper;
import site.xlinks.ai.router.mapper.ProviderModelMapper;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RouteCacheServiceTest {

    @Test
    void shouldMarkProviderUnavailableAfterMoreThanThreeConsecutiveFailures() {
        RouteCacheService service = buildService();

        service.recordProviderFailure(100L);
        service.recordProviderFailure(100L);
        service.recordProviderFailure(100L);

        assertFalse(service.isProviderTemporarilyUnavailable(100L));

        service.recordProviderFailure(100L);

        assertTrue(service.isProviderTemporarilyUnavailable(100L));
    }

    @Test
    void shouldClearProviderFailureStateAfterSuccessfulResponse() {
        RouteCacheService service = buildService();

        for (int i = 0; i < 4; i++) {
            service.recordProviderFailure(100L);
        }
        assertTrue(service.isProviderTemporarilyUnavailable(100L));

        service.clearProviderFailure(100L);

        assertFalse(service.isProviderTemporarilyUnavailable(100L));
    }

    @Test
    void shouldKeepProviderUnavailableAtTenMinutesAndExpireAfterThat() {
        RouteCacheService service = buildService();
        Instant firstFailureAt = Instant.parse("2026-04-21T00:00:00Z");
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(firstFailureAt, ZoneOffset.UTC));

        for (int i = 0; i < 4; i++) {
            service.recordProviderFailure(100L);
        }
        assertTrue(service.isProviderTemporarilyUnavailable(100L));

        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(firstFailureAt.plusSeconds(600), ZoneOffset.UTC));
        assertTrue(service.isProviderTemporarilyUnavailable(100L));

        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(firstFailureAt.plusSeconds(601), ZoneOffset.UTC));
        assertFalse(service.isProviderTemporarilyUnavailable(100L));
    }

    @Test
    void shouldCleanupExpiredProviderFailuresWhenCleanupRuns() {
        RouteCacheService service = buildService();
        Instant firstFailureAt = Instant.parse("2026-04-21T00:00:00Z");
        ReflectionTestUtils.setField(service, "clock", Clock.fixed(firstFailureAt, ZoneOffset.UTC));

        for (int i = 0; i < 4; i++) {
            service.recordProviderFailure(100L);
        }
        assertTrue(service.isProviderTemporarilyUnavailable(100L));

        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(firstFailureAt.plusSeconds(601), ZoneOffset.UTC));
        ReflectionTestUtils.invokeMethod(service, "cleanupExpiredProviderFailures");

        assertFalse(service.isProviderTemporarilyUnavailable(100L));
    }

    private RouteCacheService buildService() {
        return new RouteCacheService(
                mock(ModelMapper.class),
                mock(PlanMapper.class),
                mock(ProviderMapper.class),
                mock(ProviderModelMapper.class),
                mock(ProviderTokenMapper.class),
                mock(CustomerTokenMapper.class),
                new ObjectMapper()
        );
    }
}
