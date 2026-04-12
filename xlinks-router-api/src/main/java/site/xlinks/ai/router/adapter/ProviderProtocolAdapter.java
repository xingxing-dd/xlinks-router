package site.xlinks.ai.router.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.ProxyProtocol;
import site.xlinks.ai.router.dto.ProxyRequest;
import site.xlinks.ai.router.dto.StreamEvent;

import java.util.function.Consumer;

/**
 * Unified provider adapter abstraction for protocol-based proxy forwarding.
 */
public interface ProviderProtocolAdapter {

    /**
     * Whether the adapter supports the specified protocol.
     */
    boolean supports(ProxyProtocol protocol);

    /**
     * Forward a non-streaming request.
     */
    JsonNode forward(ProxyRequest request, ProviderInvokeContext context);

    /**
     * Forward a streaming request.
     */
    default void forwardStream(ProxyRequest request,
                               ProviderInvokeContext context,
                               Consumer<StreamEvent> onEvent) {
        throw new UnsupportedOperationException("Stream not supported yet");
    }
}

