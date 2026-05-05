package site.xlinks.ai.router.distributed.protocol.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

import java.util.Map;
import java.util.Set;

@Component
public class OpenAiProtocolRequestParsingStrategy extends AbstractProtocolRequestParsingStrategy {

    private static final Set<ForwardProtocol> SUPPORTED_PROTOCOLS = Set.of(
            ForwardProtocol.COMPLETIONS,
            ForwardProtocol.CHAT_COMPLETIONS,
            ForwardProtocol.RESPONSES
    );

    public OpenAiProtocolRequestParsingStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Set<ForwardProtocol> supportedProtocols() {
        return SUPPORTED_PROTOCOLS;
    }

    @Override
    public ForwardRequest parse(ForwardProtocol protocol,
                                String requestBody,
                                jakarta.servlet.http.HttpServletRequest request,
                                CustomerTokenResolver.ResolvedCustomerToken token) {
        String rawBody = StringUtils.defaultString(requestBody);
        JsonNode payload = parseJson(requestBody);
        return ForwardRequest.builder()
                .protocol(protocol)
                .model(readRequiredText(payload, "model"))
                .stream(readBoolean(payload, "stream"))
                .customerToken(token.value())
                .tokenSource(token.source())
                .payload(payload)
                .requestBody(rawBody)
                .passthroughHeaders(Map.of())
                .build();
    }
}
