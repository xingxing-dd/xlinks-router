package site.xlinks.ai.router.distributed.protocol.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingApplicationService;
import site.xlinks.ai.router.distributed.protocol.controller.adapter.ProtocolControllerResponseWriter;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestParser;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogType;

import java.io.IOException;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class AnthropicProtocolController {

    private final ForwardingApplicationService forwardingApplicationService;
    private final ProtocolRequestParser protocolRequestParser;
    private final ProtocolControllerResponseWriter protocolControllerResponseWriter;

    @PostMapping("/messages")
    public void messages(HttpServletRequest request,
                         HttpServletResponse response,
                         @RequestBody String requestBody) throws IOException {
        ForwardRequest forwardRequest = protocolRequestParser.parse(ForwardProtocol.ANTHROPIC_MESSAGES, requestBody, request);
        RequestChainLogCollector.bindForwardRequest(forwardRequest);
        RequestChainLogCollector.record(
                RequestChainLogType.PROTOCOL_REQUEST_RECEIVED,
                ForwardProtocol.ANTHROPIC_MESSAGES.getCode(),
                forwardRequest.getModel(),
                forwardRequest.isStream(),
                forwardRequest.getTokenSource()
        );
        protocolControllerResponseWriter.write(forwardingApplicationService.forward(forwardRequest), request, response);
    }
}
