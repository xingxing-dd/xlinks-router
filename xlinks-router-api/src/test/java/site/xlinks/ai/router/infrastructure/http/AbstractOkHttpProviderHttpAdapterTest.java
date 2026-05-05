package site.xlinks.ai.router.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.app.forwarding.model.ForwardingStage;
import site.xlinks.ai.router.app.forwarding.model.ProviderPermitLease;
import site.xlinks.ai.router.app.forwarding.model.ProviderRuntimePolicy;
import site.xlinks.ai.router.domain.routing.model.RoutingDecision;
import site.xlinks.ai.router.infrastructure.http.model.ProviderInvokeContext;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractOkHttpProviderHttpAdapterTest {

    private final TestAdapter adapter = new TestAdapter();

    @Test
    void shouldAvoidDuplicatingVersionPrefixWhenBaseUrlAlreadyContainsIt() {
        String url = adapter.buildRequestUrl("https://api.openai.com/v1", "/v1/chat/completions");

        assertEquals("https://api.openai.com/v1/chat/completions", url);
    }

    @Test
    void shouldPreserveGatewayPrefixWhenBaseUrlContainsExtraPath() {
        String url = adapter.buildRequestUrl("https://gateway.example.com/openai/v1", "/v1/chat/completions");

        assertEquals("https://gateway.example.com/openai/v1/chat/completions", url);
    }

    @Test
    void shouldAppendProviderPathWhenBaseUrlHasNoOverlap() {
        String url = adapter.buildRequestUrl("https://gateway.example.com/openai", "/v1/chat/completions");

        assertEquals("https://gateway.example.com/openai/v1/chat/completions", url);
    }

    @Test
    void shouldBuildContextWithProviderRuntimeTimeouts() {
        ForwardingDecision decision = ForwardingDecision.builder()
                .requestId("req-1")
                .selectedRoute(selectedRoute(provider(9000, 9001, 9002)))
                .providerPermitLease(new ProviderPermitLease(
                        1001L,
                        2001L,
                        "token-1",
                        "permit-1",
                        new ProviderRuntimePolicy(true, 2, 3000, 4000, 5000, 6000, 7000, 8000)
                ))
                .stage(ForwardingStage.PERMIT_ACQUIRED)
                .build();

        ProviderInvokeContext context = adapter.buildContext(decision);

        assertEquals(4000, context.getRequestTimeoutMs());
        assertEquals(5000, context.getStreamFirstResponseTimeoutMs());
        assertEquals(6000, context.getStreamIdleTimeoutMs());
    }

    @Test
    void shouldFallbackToProviderTimeoutsWhenPermitPolicyMissing() {
        ForwardingDecision decision = ForwardingDecision.builder()
                .requestId("req-2")
                .selectedRoute(selectedRoute(provider(1100, 2200, 3300)))
                .build();

        ProviderInvokeContext context = adapter.buildContext(decision);

        assertEquals(1100, context.getRequestTimeoutMs());
        assertEquals(2200, context.getStreamFirstResponseTimeoutMs());
        assertEquals(3300, context.getStreamIdleTimeoutMs());
    }

    private static final class TestAdapter extends AbstractOkHttpProviderHttpAdapter {

        private TestAdapter() {
            super(new OkHttpClient(), new ObjectMapper());
        }

        @Override
        protected Request buildRequest(ForwardingDecision decision, ProviderInvokeContext context) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean supports(ForwardProtocol protocol) {
            return false;
        }
    }

    private RoutingDecision selectedRoute(Provider provider) {
        return RoutingDecision.builder()
                .provider(provider)
                .providerToken(providerToken())
                .providerModel(providerModel())
                .build();
    }

    private Provider provider(int requestTimeoutMs, int streamFirstResponseTimeoutMs, int streamIdleTimeoutMs) {
        Provider provider = new Provider();
        provider.setId(1001L);
        provider.setProviderCode("openai");
        provider.setProviderName("OpenAI");
        provider.setBaseUrl("https://example.com");
        provider.setRequestTimeoutMs(requestTimeoutMs);
        provider.setStreamFirstResponseTimeoutMs(streamFirstResponseTimeoutMs);
        provider.setStreamIdleTimeoutMs(streamIdleTimeoutMs);
        return provider;
    }

    private ProviderToken providerToken() {
        ProviderToken providerToken = new ProviderToken();
        providerToken.setId(2001L);
        providerToken.setTokenValue("sk-test");
        return providerToken;
    }

    private ProviderModel providerModel() {
        ProviderModel providerModel = new ProviderModel();
        providerModel.setProviderModelCode("gpt-test");
        return providerModel;
    }
}
