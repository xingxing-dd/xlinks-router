package site.xlinks.ai.router.protocol.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.app.forwarding.ForwardingApplicationService;
import site.xlinks.ai.router.protocol.controller.adapter.ProtocolControllerResponseWriter;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;
import site.xlinks.ai.router.protocol.service.OpenAiModelsService;
import site.xlinks.ai.router.protocol.service.ProtocolRequestParser;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

import java.io.IOException;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OpenAiProtocolController {

    private final ForwardingApplicationService forwardingApplicationService;
    private final ProtocolRequestParser protocolRequestParser;
    private final ProtocolControllerResponseWriter protocolControllerResponseWriter;
    private final OpenAiModelsService openAiModelsService;

    @PostMapping("/completions")
    public void completions(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
        acceptOpenAiRequest(request, response, ForwardProtocol.COMPLETIONS);
    }

    @PostMapping("/chat/completions")
    public void chatCompletions(HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        acceptOpenAiRequest(request, response, ForwardProtocol.CHAT_COMPLETIONS);
    }

    @PostMapping("/responses")
    public void responses(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        acceptOpenAiRequest(request, response, ForwardProtocol.RESPONSES);
    }

    @GetMapping("/models")
    public Object models(HttpServletRequest request) {
        RequestChainLogCollector.record(RequestChainLogType.MODELS_REQUEST_RECEIVED);
        return openAiModelsService.listModels(request);
    }

    private void acceptOpenAiRequest(HttpServletRequest request,
                                     HttpServletResponse response,
                                     ForwardProtocol protocol) throws IOException {
        RequestChainLogCollector.record(RequestChainLogType.PROTOCOL_CONTROLLER_ENTERED, protocol.getCode());
        ForwardRequest forwardRequest = protocolRequestParser.parse(protocol, request);
        RequestChainLogCollector.bindForwardRequest(forwardRequest);
        RequestChainLogCollector.record(
                RequestChainLogType.PROTOCOL_REQUEST_RECEIVED,
                protocol.getCode(),
                forwardRequest.getModel(),
                forwardRequest.isStream(),
                forwardRequest.getTokenSource()
        );
        protocolControllerResponseWriter.write(forwardingApplicationService.forward(forwardRequest), request, response);
    }
}
