package site.xlinks.ai.router.adapter;

import org.springframework.stereotype.Component;
import site.xlinks.ai.router.dto.ProxyProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves protocol adapters by request protocol.
 */
@Component
public class ProviderProtocolAdapterFactory {

    private final List<ProviderProtocolAdapter> adapters;
    private final Map<String, ProviderProtocolAdapter> adapterCache = new ConcurrentHashMap<>();

    public ProviderProtocolAdapterFactory(List<ProviderProtocolAdapter> adapters) {
        this.adapters = adapters;
    }

    public ProviderProtocolAdapter getAdapter(ProxyProtocol protocol) {
        if (protocol == null) {
            return null;
        }
        String protocolCode = protocol.getCode();
        ProviderProtocolAdapter cached = adapterCache.get(protocolCode);
        if (cached != null) {
            return cached;
        }

        for (ProviderProtocolAdapter adapter : adapters) {
            if (adapter.supports(protocol)) {
                adapterCache.put(protocolCode, adapter);
                return adapter;
            }
        }
        return null;
    }
}

