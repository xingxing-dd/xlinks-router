package site.xlinks.ai.router.distributed.infrastructure.http;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProviderHttpForwardingExecutor {

    private final List<ProviderHttpAdapter> providerHttpAdapters;
    private volatile Map<ForwardProtocol, ProviderHttpAdapter> adapterByProtocol;

    public Object forward(ForwardingDecision decision) {
        ForwardProtocol protocol = decision.getRequest().getProtocol();
        ProviderHttpAdapter adapter = getAdapterByProtocol().get(protocol);
        if (adapter == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "没有可用的服务商 HTTP 适配器");
        }
        return adapter.forward(decision);
    }

    private Map<ForwardProtocol, ProviderHttpAdapter> getAdapterByProtocol() {
        Map<ForwardProtocol, ProviderHttpAdapter> local = adapterByProtocol;
        if (local == null) {
            local = buildAdapterMap(providerHttpAdapters);
            adapterByProtocol = local;
        }
        return local;
    }

    private Map<ForwardProtocol, ProviderHttpAdapter> buildAdapterMap(List<ProviderHttpAdapter> adapters) {
        Map<ForwardProtocol, ProviderHttpAdapter> mappings = new EnumMap<>(ForwardProtocol.class);
        for (ForwardProtocol protocol : ForwardProtocol.values()) {
            for (ProviderHttpAdapter adapter : adapters) {
                if (!adapter.supports(protocol)) {
                    continue;
                }
                ProviderHttpAdapter existing = mappings.putIfAbsent(protocol, adapter);
                if (existing != null) {
                    throw new IllegalStateException("Duplicate provider http adapter registered for " + protocol);
                }
            }
        }
        return Map.copyOf(mappings);
    }
}
