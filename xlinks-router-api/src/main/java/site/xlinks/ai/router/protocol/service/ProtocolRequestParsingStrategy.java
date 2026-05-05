package site.xlinks.ai.router.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

import java.util.Set;

public interface ProtocolRequestParsingStrategy {

    Set<ForwardProtocol> supportedProtocols();

    ForwardRequest parse(ForwardProtocol protocol,
                         String requestBody,
                         HttpServletRequest request,
                         CustomerTokenResolver.ResolvedCustomerToken token);
}
