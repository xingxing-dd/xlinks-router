package site.xlinks.ai.router.support.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestChainLogCollectorTest {

    @Test
    void shouldRenderModelsRequestAsQueryLog() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v1/models");

        RequestChainLogCollector.RequestChainLogSession session = RequestChainLogCollector.start(request, "trace-1");
        try {
            RequestChainLogCollector.bindProtocolContext("models", "AUTHORIZATION_BEARER", null);

            CustomerToken customerToken = new CustomerToken();
            customerToken.setId(11L);
            customerToken.setCustomerName("merchant-a");
            customerToken.setTokenName("default-token");

            CustomerAccount customerAccount = new CustomerAccount();
            customerAccount.setId(22L);
            customerAccount.setUsername("account-a");

            RequestChainLogCollector.bindCustomer(customerToken, customerAccount, null);
            RequestChainLogCollector.bindQueryResultCount(3);
            RequestChainLogCollector.markResponseStatus(200);
            RequestChainLogCollector.markSuccess("模型列表查询成功");
            RequestChainLogCollector.record(RequestChainLogType.MODELS_REQUEST_RECEIVED);

            String rendered = render(session);
            assertTrue(rendered.contains("模型列表查询日志"));
            assertTrue(rendered.contains("protocol=models"));
            assertTrue(rendered.contains("tokenSource=AUTHORIZATION_BEARER"));
            assertTrue(rendered.contains("返回模型数=3"));
            assertFalse(rendered.contains("requestId="));
            assertFalse(rendered.contains("服务商="));
            assertFalse(rendered.contains("服务商令牌="));
        } finally {
            RequestChainLogCollector.clear();
        }
    }

    @Test
    void shouldRenderBalanceRequestAsQueryLog() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/user/balance");

        RequestChainLogCollector.RequestChainLogSession session = RequestChainLogCollector.start(request, "trace-2");
        try {
            RequestChainLogCollector.bindProtocolContext("balance", "AUTHORIZATION_BEARER", Boolean.FALSE);

            CustomerToken customerToken = new CustomerToken();
            customerToken.setId(12L);
            customerToken.setCustomerName("merchant-b");
            customerToken.setTokenName("balance-token");

            CustomerAccount customerAccount = new CustomerAccount();
            customerAccount.setId(23L);
            customerAccount.setUsername("account-b");

            RequestChainLogCollector.bindCustomer(customerToken, customerAccount, null);
            RequestChainLogCollector.bindQueryBalance(new BigDecimal("45.67"));
            RequestChainLogCollector.markResponseStatus(200);
            RequestChainLogCollector.markSuccess("Balance query succeeded");

            String rendered = render(session);
            assertTrue(rendered.contains("Balance Query Log"));
            assertTrue(rendered.contains("protocol=balance"));
            assertTrue(rendered.contains("tokenSource=AUTHORIZATION_BEARER"));
            assertTrue(rendered.contains("45.67 USD"));
            assertFalse(rendered.contains("requestId="));
            assertFalse(rendered.contains("鏈嶅姟鍟?"));
            assertFalse(rendered.contains("鏈嶅姟鍟嗕护鐗?"));
        } finally {
            RequestChainLogCollector.clear();
        }
    }

    @Test
    void shouldRenderSimpleHttpRequestAsCompactLog() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/");

        RequestChainLogCollector.RequestChainLogSession session = RequestChainLogCollector.start(request, "trace-3");
        try {
            RequestChainLogCollector.markResponseStatus(404);
            RequestChainLogCollector.markBusinessFailure("request path not found");

            String rendered = render(session);
            assertTrue(rendered.contains("method=GET"));
            assertTrue(rendered.contains("uri=/"));
            assertTrue(rendered.contains("traceId=trace-3"));
            assertFalse(rendered.contains("requestId="));
            assertFalse(rendered.contains("protocol="));
            assertFalse(rendered.contains("stream="));
            assertFalse(rendered.contains("tokenSource="));
            assertFalse(rendered.contains("customerAccount="));
            assertFalse(rendered.contains("customerToken="));
            assertFalse(rendered.contains("业务摘要"));
            assertFalse(rendered.contains("关键节点"));
        } finally {
            RequestChainLogCollector.clear();
        }
    }

    private String render(RequestChainLogCollector.RequestChainLogSession session) throws Exception {
        Method renderMethod = session.getClass().getDeclaredMethod("render");
        renderMethod.setAccessible(true);
        return (String) renderMethod.invoke(session);
    }
}
