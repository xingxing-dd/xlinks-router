package site.xlinks.ai.router.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import site.xlinks.ai.router.client.dto.auth.AuthRegisterRequest;
import site.xlinks.ai.router.client.dto.auth.AuthResetPasswordRequest;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAccountServiceTest {

    @Mock
    private CustomerAccountMapper customerAccountMapper;

    @Mock
    private PromotionService promotionService;

    @Mock
    private TokenService tokenService;

    @Mock
    private VerifyCodeService verifyCodeService;

    private CustomerAccountService customerAccountService;

    @BeforeEach
    void setUp() {
        customerAccountService = new CustomerAccountService(
                customerAccountMapper,
                promotionService,
                tokenService,
                verifyCodeService
        );
    }

    @Test
    void shouldRegisterAfterVerifyingCode() {
        when(customerAccountMapper.selectOne(any())).thenReturn(null);

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setTarget("13800138000");
        request.setTargetType("phone");
        request.setPassword("new-password");
        request.setCode("123456");
        request.setInviteCode("INVITE2026");

        customerAccountService.register(request);

        ArgumentCaptor<CustomerAccount> accountCaptor = ArgumentCaptor.forClass(CustomerAccount.class);
        verify(verifyCodeService).verifyOrThrow("register", "phone", "13800138000", "123456");
        verify(customerAccountMapper).insert(accountCaptor.capture());
        verify(promotionService).bindInviterAndInitReward(accountCaptor.getValue(), "INVITE2026");

        CustomerAccount saved = accountCaptor.getValue();
        assertEquals("13800138000", saved.getUsername());
        assertEquals("13800138000", saved.getPhone());
        assertTrue(new BCryptPasswordEncoder().matches("new-password", saved.getPassword()));
    }

    @Test
    void shouldResetPasswordAfterVerifyingCode() {
        CustomerAccount account = new CustomerAccount();
        account.setId(1L);
        account.setEmail("user@example.com");
        account.setPassword(new BCryptPasswordEncoder().encode("old-password"));
        when(customerAccountMapper.selectOne(any())).thenReturn(account);

        AuthResetPasswordRequest request = new AuthResetPasswordRequest();
        request.setTarget("user@example.com");
        request.setTargetType("email");
        request.setPassword("new-password");
        request.setCode("654321");

        customerAccountService.resetPassword(request);

        verify(verifyCodeService).verifyOrThrow("resetpwd", "email", "user@example.com", "654321");
        verify(customerAccountMapper).updateById(account);
        assertTrue(new BCryptPasswordEncoder().matches("new-password", account.getPassword()));
    }

    @Test
    void shouldRejectResetVerifyCodeForUnknownAccount() {
        when(customerAccountMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> customerAccountService.validateVerifyCodeTarget("resetpwd", "email", "missing@example.com")
        );

        assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
    }
}
