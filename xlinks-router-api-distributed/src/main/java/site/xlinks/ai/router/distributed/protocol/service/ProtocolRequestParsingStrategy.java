package site.xlinks.ai.router.distributed.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

import java.util.Set;

public interface ProtocolRequestParsingStrategy {

    Set<ForwardProtocol> supportedProtocols();

    ForwardRequest parse(ForwardProtocol protocol,
                         String requestBody,
                         HttpServletRequest request,
                         CustomerTokenResolver.ResolvedCustomerToken token);
}
