package site.xlinks.ai.router.adapter;

import org.springframework.stereotype.Component;
import site.xlinks.ai.router.dto.ProxyProtocol;

/**
 * @deprecated Use {@link ProviderProtocolAdapterFactory} instead.
 */
@Deprecated
@Component
public class OpenAIProviderAdapterFactory {

    private final ProviderProtocolAdapterFactory delegate;

    public OpenAIProviderAdapterFactory(ProviderProtocolAdapterFactory delegate) {
        this.delegate = delegate;
    }

    public ProviderProtocolAdapter getAdapter(ProxyProtocol protocol) {
        return delegate.getAdapter(protocol);
    }
}

