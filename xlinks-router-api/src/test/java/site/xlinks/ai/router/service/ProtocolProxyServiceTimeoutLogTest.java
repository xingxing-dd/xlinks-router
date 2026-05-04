package site.xlinks.ai.router.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolProxyServiceTimeoutLogTest {

    private final ProtocolProxyService service = new ProtocolProxyService(
            null, null, null, null, null, null, null, null, null
    );

    @Test
    void shouldMarkNonStreamTimeoutFieldsAsConfigOnly() {
        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .baseUrl("https://example.com/v1")
                .providerToken("provider-token")
                .requestTimeoutMs(20_000)
                .streamFirstResponseTimeoutMs(40_000)
                .streamIdleTimeoutMs(40_000)
                .build();
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.CHAT_COMPLETIONS)
                .stream(false)
                .build();

        String detail = ReflectionTestUtils.invokeMethod(service, "buildUpstreamRequestDetail", context, request, false);

        assertTrue(detail.contains("effectiveRequestTimeoutMs=20000"));
        assertTrue(detail.contains("streamFirstResponseTimeoutMs(configOnlyForStream)=40000"));
        assertTrue(detail.contains("streamIdleTimeoutMs(configOnlyForStream)=40000"));
        assertFalse(detail.contains("effectiveFirstResponseTimeoutMs"));
    }

    @Test
    void shouldMarkStreamTimeoutFieldsAsEffective() {
        ProviderInvokeContext context = ProviderInvokeContext.builder()
                .baseUrl("https://example.com/v1")
                .providerToken("provider-token")
                .requestTimeoutMs(20_000)
                .streamFirstResponseTimeoutMs(40_000)
                .streamIdleTimeoutMs(55_000)
                .build();
        ProxyRequest request = ProxyRequest.builder()
                .protocol(ProxyProtocol.CHAT_COMPLETIONS)
                .stream(true)
                .build();

        String detail = ReflectionTestUtils.invokeMethod(service, "buildUpstreamRequestDetail", context, request, true);

        assertTrue(detail.contains("effectiveFirstResponseTimeoutMs=40000"));
        assertTrue(detail.contains("effectiveIdleTimeoutMs=55000"));
        assertTrue(detail.contains("requestTimeoutMs(configOnlyForNonStream)=20000"));
        assertFalse(detail.contains("effectiveRequestTimeoutMs"));
    }

    @Test
    void shouldDetectClientAbortFromNestedBrokenPipeMessage() {
        boolean result = ReflectionTestUtils.invokeMethod(
                service,
                "isClientAbortException",
                new RuntimeException("wrapper", new RuntimeException("Broken pipe"))
        );

        assertTrue(result);
    }
}
