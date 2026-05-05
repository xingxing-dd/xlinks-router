package site.xlinks.ai.router.infrastructure.http;

import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;

public interface ProviderHttpAdapter {

    boolean supports(ForwardProtocol protocol);

    Object forward(ForwardingDecision decision);
}
