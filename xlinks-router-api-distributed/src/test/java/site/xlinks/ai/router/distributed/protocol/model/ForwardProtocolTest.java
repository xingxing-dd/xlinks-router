package site.xlinks.ai.router.distributed.protocol.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForwardProtocolTest {

    @Test
    void shouldResolveProtocolByRequestUri() {
        assertEquals(
                ForwardProtocol.CHAT_COMPLETIONS,
                ForwardProtocol.fromRequestUri("/v1/chat/completions").orElseThrow()
        );
        assertEquals(
                ForwardProtocol.RESPONSES,
                ForwardProtocol.fromRequestUri("/v1/responses").orElseThrow()
        );
        assertEquals(
                ForwardProtocol.ANTHROPIC_MESSAGES,
                ForwardProtocol.fromRequestUri("/v1/messages").orElseThrow()
        );
    }

    @Test
    void shouldReturnEmptyWhenRequestUriIsUnknown() {
        assertTrue(ForwardProtocol.fromRequestUri("/v1/unknown").isEmpty());
    }
}
