package site.xlinks.ai.router.adapter;

import org.springframework.stereotype.Component;
import site.xlinks.ai.router.dto.OpenAIProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves OpenAI-compatible provider adapters by request protocol.
 */
@Component
public class OpenAIProviderAdapterFactory {

    private final List<OpenAIProviderAdapter> providerAdapters;
    private final Map<String, OpenAIProviderAdapter> adapterCache = new ConcurrentHashMap<>();

    public OpenAIProviderAdapterFactory(List<OpenAIProviderAdapter> providerAdapters) {
        this.providerAdapters = providerAdapters;
    }

    public OpenAIProviderAdapter getAdapter(OpenAIProtocol protocol) {
        if (protocol == null) {
            return null;
        }

        String protocolCode = protocol.getCode();
        OpenAIProviderAdapter cachedAdapter = adapterCache.get(protocolCode);
        if (cachedAdapter != null) {
            return cachedAdapter;
        }

        for (OpenAIProviderAdapter adapter : providerAdapters) {
            if (adapter.supports(protocol)) {
                adapterCache.put(protocolCode, adapter);
                return adapter;
            }
        }
        return null;
    }
}
