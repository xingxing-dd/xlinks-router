package site.xlinks.ai.router.distributed.protocol.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingApplicationService;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestContextResolver;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestParser;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiProtocolController {

    private final ForwardingApplicationService forwardingApplicationService;
    private final ProtocolRequestContextResolver protocolRequestContextResolver;
    private final ProtocolRequestParser protocolRequestParser;

    @PostMapping("/completions")
    public Object completions(HttpServletRequest request,
                              @RequestBody String requestBody) {
        return acceptOpenAiRequest(request, requestBody, ForwardProtocol.COMPLETIONS);
    }

    @PostMapping("/chat/completions")
    public Object chatCompletions(HttpServletRequest request,
                                  @RequestBody String requestBody) {
        return acceptOpenAiRequest(request, requestBody, ForwardProtocol.CHAT_COMPLETIONS);
    }

    @GetMapping("/models")
    public Object models(HttpServletRequest request) {
        CustomerTokenResolver.ResolvedCustomerToken token = protocolRequestContextResolver.resolveCustomerToken(request);
        log.debug("Accepted models request, tokenSource={}", token.source());
        return Map.of(
                "object", "list",
                "data", List.of()
        );
    }

    private Object acceptOpenAiRequest(HttpServletRequest request,
                                       String requestBody,
                                       ForwardProtocol protocol) {
        CustomerTokenResolver.ResolvedCustomerToken token = protocolRequestContextResolver.resolveCustomerToken(request);
        ForwardRequest forwardRequest = protocolRequestParser.parseOpenAi(protocol, requestBody, token);
        log.debug("Accepted {} request, model={}, stream={}, tokenSource={}",
                protocol.getCode(), forwardRequest.getModel(), forwardRequest.isStream(), token.source());
        return forwardingApplicationService.forward(forwardRequest);
    }
}
