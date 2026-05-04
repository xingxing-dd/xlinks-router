package site.xlinks.ai.router.distributed.infrastructure.http;

import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingPreparation;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;

public interface ProviderHttpAdapter {

    boolean supports(ForwardProtocol protocol);

    Object forward(ForwardingPreparation preparation);
}
