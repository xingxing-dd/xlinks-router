package site.xlinks.ai.router.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves OpenAI-compatible provider adapters by provider type.
 */
@Component
public class OpenAIProviderAdapterFactory {

    private final List<OpenAIProviderAdapter> providerAdapters;
    private final Map<String, OpenAIProviderAdapter> adapterCache = new ConcurrentHashMap<>();

    public OpenAIProviderAdapterFactory(List<OpenAIProviderAdapter> providerAdapters) {
        this.providerAdapters = providerAdapters;
    }

    public OpenAIProviderAdapter getAdapter(String providerType) {
        OpenAIProviderAdapter cachedAdapter = adapterCache.get(providerType);
        if (cachedAdapter != null) {
            return cachedAdapter;
        }

        for (OpenAIProviderAdapter adapter : providerAdapters) {
            if (adapter.supports(providerType)) {
                adapterCache.put(providerType, adapter);
                return adapter;
            }
        }
        return null;
    }
}
