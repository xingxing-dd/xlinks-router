package site.xlinks.ai.router.distributed.protocol.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingApplicationService;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestContextResolver;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestParser;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AnthropicProtocolController {

    private final ForwardingApplicationService forwardingApplicationService;
    private final ProtocolRequestContextResolver protocolRequestContextResolver;
    private final ProtocolRequestParser protocolRequestParser;

    @PostMapping("/messages")
    public Object messages(HttpServletRequest request,
                           @RequestBody String requestBody) {
        CustomerTokenResolver.ResolvedCustomerToken token = protocolRequestContextResolver.resolveCustomerToken(request);
        ForwardRequest forwardRequest = protocolRequestParser.parseAnthropic(requestBody, request, token);
        log.debug("Accepted anthropic messages request, model={}, stream={}, tokenSource={}",
                forwardRequest.getModel(), forwardRequest.isStream(), token.source());
        return forwardingApplicationService.forward(forwardRequest);
    }
}
