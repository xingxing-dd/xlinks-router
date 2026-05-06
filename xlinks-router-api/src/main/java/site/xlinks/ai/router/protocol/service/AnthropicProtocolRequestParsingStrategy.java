package site.xlinks.ai.router.protocol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Component
public class AnthropicProtocolRequestParsingStrategy extends AbstractProtocolRequestParsingStrategy {

    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String HEADER_ANTHROPIC_BETA = "anthropic-beta";

    public AnthropicProtocolRequestParsingStrategy(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    @Override
    public Set<ForwardProtocol> supportedProtocols() {
        return Set.of(ForwardProtocol.ANTHROPIC_MESSAGES);
    }

    @Override
    public ForwardRequest parse(ForwardProtocol protocol,
                                String requestBody,
                                HttpServletRequest request,
                                CustomerTokenResolver.ResolvedCustomerToken token) {
        String rawBody = StringUtils.defaultString(requestBody);
        ParsedRequestFields fields = parseRequestFields(requestBody);
        Map<String, String> passthroughHeaders = new LinkedHashMap<>();
        copyHeaderIfPresent(request, passthroughHeaders, HEADER_ANTHROPIC_VERSION);
        copyHeaderIfPresent(request, passthroughHeaders, HEADER_ANTHROPIC_BETA);
        return ForwardRequest.builder()
                .protocol(protocol)
                .model(readRequiredText(fields))
                .stream(readBoolean(fields))
                .customerToken(token.value())
                .tokenSource(token.source())
                .payload(null)
                .requestBody(rawBody)
                .passthroughHeaders(passthroughHeaders)
                .build();
    }

    private void copyHeaderIfPresent(HttpServletRequest request,
                                     Map<String, String> headers,
                                     String headerName) {
        String value = StringUtils.trimToNull(request.getHeader(headerName));
        if (value != null) {
            headers.put(headerName, value);
        }
    }
}
