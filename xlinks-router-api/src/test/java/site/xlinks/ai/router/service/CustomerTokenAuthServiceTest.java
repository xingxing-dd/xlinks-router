package site.xlinks.ai.router.service;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerTokenAuthServiceTest {

    @Test
    void shouldValidateTokenAgainstFreshDatabaseRecord() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CustomerTokenMapper customerTokenMapper = mock(CustomerTokenMapper.class);
        CustomerTokenAuthService service = new CustomerTokenAuthService(routeCacheService, customerTokenMapper);

        CustomerToken cachedToken = buildToken();
        CustomerToken freshToken = buildToken();
        freshToken.setCustomerName("fresh-demo");

        when(routeCacheService.getCustomerTokenByValue("sk-token")).thenReturn(cachedToken);
        when(customerTokenMapper.selectById(cachedToken.getId())).thenReturn(freshToken);

        CustomerToken result = service.validateToken("sk-token");

        assertSame(freshToken, result);
        verify(routeCacheService, times(1)).getCustomerTokenByValue("sk-token");
        verify(customerTokenMapper, times(1)).selectById(cachedToken.getId());
        verify(routeCacheService, times(1)).cacheCustomerToken(freshToken);
    }

    @Test
    void shouldRejectValidateTokenWhenFreshTokenValueDoesNotMatch() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CustomerTokenMapper customerTokenMapper = mock(CustomerTokenMapper.class);
        CustomerTokenAuthService service = new CustomerTokenAuthService(routeCacheService, customerTokenMapper);

        CustomerToken cachedToken = buildToken();
        CustomerToken freshToken = buildToken();
        freshToken.setTokenValue("sk-new-token");

        when(routeCacheService.getCustomerTokenByValue("sk-token")).thenReturn(cachedToken);
        when(customerTokenMapper.selectById(cachedToken.getId())).thenReturn(freshToken);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.validateToken("sk-token"));

        assertEquals("Invalid token", exception.getMessage());
        verify(routeCacheService, times(1)).getCustomerTokenByValue("sk-token");
        verify(customerTokenMapper, times(1)).selectById(cachedToken.getId());
        verify(routeCacheService, never()).cacheCustomerToken(freshToken);
    }

    @Test
    void shouldRejectWhenCachedQuotaAlreadyReached() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CustomerTokenMapper customerTokenMapper = mock(CustomerTokenMapper.class);
        CustomerTokenAuthService service = new CustomerTokenAuthService(routeCacheService, customerTokenMapper);

        CustomerToken token = buildToken();
        token.setDailyQuota(new BigDecimal("10"));
        token.setUsedQuota(new BigDecimal("10"));

        when(routeCacheService.getCustomerTokenByValue("sk-token")).thenReturn(token);
        when(routeCacheService.isCustomerTokenModelAllowed(token, "gpt-4o")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.validateRequestAccess("sk-token", "gpt-4o"));

        assertEquals("Customer token daily quota reached", exception.getMessage());
        verify(routeCacheService, times(1)).getCustomerTokenByValue("sk-token");
        verify(customerTokenMapper, times(0)).selectById(token.getId());
    }

    @Test
    void shouldRejectWhenFreshDatabaseQuotaAlreadyReached() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CustomerTokenMapper customerTokenMapper = mock(CustomerTokenMapper.class);
        CustomerTokenAuthService service = new CustomerTokenAuthService(routeCacheService, customerTokenMapper);

        CustomerToken cachedToken = buildToken();
        cachedToken.setDailyQuota(new BigDecimal("10"));
        cachedToken.setUsedQuota(new BigDecimal("8"));

        CustomerToken freshToken = buildToken();
        freshToken.setDailyQuota(new BigDecimal("10"));
        freshToken.setUsedQuota(new BigDecimal("10"));

        when(routeCacheService.getCustomerTokenByValue("sk-token")).thenReturn(cachedToken);
        when(customerTokenMapper.selectById(cachedToken.getId())).thenReturn(freshToken);
        when(routeCacheService.isCustomerTokenModelAllowed(cachedToken, "gpt-4o")).thenReturn(true);
        when(routeCacheService.isCustomerTokenModelAllowed(freshToken, "gpt-4o")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.validateRequestAccess("sk-token", "gpt-4o"));

        assertEquals("Customer token daily quota reached", exception.getMessage());
        verify(routeCacheService, times(1)).getCustomerTokenByValue("sk-token");
        verify(customerTokenMapper, times(1)).selectById(cachedToken.getId());
    }

    @Test
    void shouldReturnFreshCustomerTokenWhenAccessAllowed() {
        RouteCacheService routeCacheService = mock(RouteCacheService.class);
        CustomerTokenMapper customerTokenMapper = mock(CustomerTokenMapper.class);
        CustomerTokenAuthService service = new CustomerTokenAuthService(routeCacheService, customerTokenMapper);

        CustomerToken cachedToken = buildToken();
        cachedToken.setDailyQuota(new BigDecimal("10"));
        cachedToken.setUsedQuota(new BigDecimal("8"));

        CustomerToken freshToken = buildToken();
        freshToken.setDailyQuota(new BigDecimal("10"));
        freshToken.setUsedQuota(new BigDecimal("9"));

        when(routeCacheService.getCustomerTokenByValue("sk-token")).thenReturn(cachedToken);
        when(customerTokenMapper.selectById(cachedToken.getId())).thenReturn(freshToken);
        when(routeCacheService.isCustomerTokenModelAllowed(cachedToken, "gpt-4o")).thenReturn(true);
        when(routeCacheService.isCustomerTokenModelAllowed(freshToken, "gpt-4o")).thenReturn(true);

        CustomerToken result = service.validateRequestAccess("sk-token", "gpt-4o");

        assertSame(freshToken, result);
        verify(routeCacheService, times(1)).getCustomerTokenByValue("sk-token");
        verify(customerTokenMapper, times(1)).selectById(cachedToken.getId());
        verify(routeCacheService, times(1)).cacheCustomerToken(freshToken);
    }

    private CustomerToken buildToken() {
        CustomerToken token = new CustomerToken();
        token.setId(1L);
        token.setAccountId(100L);
        token.setCustomerName("demo");
        token.setTokenValue("sk-token");
        token.setStatus(1);
        token.setExpireTime(LocalDateTime.now().plusMinutes(10));
        token.setAllowedModels("[\"gpt-4o\"]");
        token.setTotalQuota(new BigDecimal("100"));
        token.setTotalUsedQuota(new BigDecimal("1"));
        return token;
    }
}
