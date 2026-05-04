package site.xlinks.ai.router.distributed.infrastructure.http;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingPreparation;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProviderHttpForwardingExecutor {

    private final List<ProviderHttpAdapter> providerHttpAdapters;

    public Object forward(ForwardingPreparation preparation) {
        ForwardProtocol protocol = preparation.getRequest().getProtocol();
        return providerHttpAdapters.stream()
                .filter(adapter -> adapter.supports(protocol))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "No provider http adapter available"))
                .forward(preparation);
    }
}
