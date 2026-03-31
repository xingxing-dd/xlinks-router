package site.xlinks.ai.router.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import site.xlinks.ai.router.context.ProviderInvokeContext;
import site.xlinks.ai.router.dto.OpenAIProxyRequest;
import site.xlinks.ai.router.dto.OpenAIStreamEvent;

/**
 * Provider adapter abstraction for OpenAI-compatible proxy calls.
 */
public interface OpenAIProviderAdapter {

    /**
     * Whether the adapter supports the provider type.
     */
    boolean supports(String providerType);

    /**
     * Forward a non-streaming OpenAI-compatible request.
     */
    JsonNode forward(OpenAIProxyRequest request, ProviderInvokeContext context);

    /**
     * Forward a streaming OpenAI-compatible request.
     */
    default void forwardStream(OpenAIProxyRequest request,
                               ProviderInvokeContext context,
                               java.util.function.Consumer<OpenAIStreamEvent> onEvent) {
        throw new UnsupportedOperationException("Stream not supported yet");
    }
}
