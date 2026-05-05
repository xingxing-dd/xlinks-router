package site.xlinks.ai.router.infrastructure.http;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderHttpForwardingExecutorTest {

    @Test
    void shouldDispatchToMatchingAdapter() {
        ProviderHttpForwardingExecutor executor = new ProviderHttpForwardingExecutor(List.of(
                new StubProviderHttpAdapter(ForwardProtocol.ANTHROPIC_MESSAGES, "anthropic"),
                new StubProviderHttpAdapter(ForwardProtocol.CHAT_COMPLETIONS, "openai-chat")
        ));

        Object response = executor.forward(decisionOf(ForwardProtocol.CHAT_COMPLETIONS));

        assertEquals("openai-chat", response);
    }

    @Test
    void shouldRejectWhenNoAdapterSupportsProtocol() {
        ProviderHttpForwardingExecutor executor = new ProviderHttpForwardingExecutor(List.of(
                new StubProviderHttpAdapter(ForwardProtocol.ANTHROPIC_MESSAGES, "anthropic")
        ));

        assertThrows(BusinessException.class, () -> executor.forward(decisionOf(ForwardProtocol.CHAT_COMPLETIONS)));
    }

    @Test
    void shouldRejectDuplicateAdapterRegistration() {
        ProviderHttpForwardingExecutor executor = new ProviderHttpForwardingExecutor(List.of(
                new StubProviderHttpAdapter(ForwardProtocol.CHAT_COMPLETIONS, "adapter-1"),
                new StubProviderHttpAdapter(ForwardProtocol.CHAT_COMPLETIONS, "adapter-2")
        ));

        assertThrows(IllegalStateException.class, () -> executor.forward(decisionOf(ForwardProtocol.CHAT_COMPLETIONS)));
    }

    private ForwardingDecision decisionOf(ForwardProtocol protocol) {
        return ForwardingDecision.builder()
                .request(ForwardRequest.builder().protocol(protocol).build())
                .build();
    }

    private record StubProviderHttpAdapter(ForwardProtocol protocol, Object response) implements ProviderHttpAdapter {

        @Override
        public boolean supports(ForwardProtocol candidate) {
            return protocol == candidate;
        }

        @Override
        public Object forward(ForwardingDecision decision) {
            return response;
        }
    }
}
