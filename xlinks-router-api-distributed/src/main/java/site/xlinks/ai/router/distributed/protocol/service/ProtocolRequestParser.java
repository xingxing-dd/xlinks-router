package site.xlinks.ai.router.distributed.protocol.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProtocolRequestParser {

    private static final String HEADER_ANTHROPIC_VERSION = "anthropic-version";
    private static final String HEADER_ANTHROPIC_BETA = "anthropic-beta";

    private final ObjectMapper objectMapper;

    public ForwardRequest parseOpenAi(ForwardProtocol protocol,
                                      String requestBody,
                                      CustomerTokenResolver.ResolvedCustomerToken token) {
        String rawBody = StringUtils.defaultString(requestBody);
        JsonNode payload = parseJson(requestBody);
        String model = readRequiredText(payload, "model");
        Boolean stream = readBoolean(payload, "stream");
        return ForwardRequest.builder()
                .protocol(protocol)
                .model(model)
                .stream(stream)
                .customerToken(token.value())
                .tokenSource(token.source())
                .payload(payload)
                .requestBody(rawBody)
                .passthroughHeaders(Map.of())
                .build();
    }

    public ForwardRequest parseAnthropic(String requestBody,
                                         HttpServletRequest request,
                                         CustomerTokenResolver.ResolvedCustomerToken token) {
        String rawBody = StringUtils.defaultString(requestBody);
        JsonNode payload = parseJson(requestBody);
        String model = readRequiredText(payload, "model");
        Boolean stream = readBoolean(payload, "stream");
        Map<String, String> passthroughHeaders = new LinkedHashMap<>();
        copyHeaderIfPresent(request, passthroughHeaders, HEADER_ANTHROPIC_VERSION);
        copyHeaderIfPresent(request, passthroughHeaders, HEADER_ANTHROPIC_BETA);
        return ForwardRequest.builder()
                .protocol(ForwardProtocol.ANTHROPIC_MESSAGES)
                .model(model)
                .stream(stream)
                .customerToken(token.value())
                .tokenSource(token.source())
                .payload(payload)
                .requestBody(rawBody)
                .passthroughHeaders(passthroughHeaders)
                .build();
    }

    private JsonNode parseJson(String requestBody) {
        try {
            return objectMapper.readTree(StringUtils.defaultIfBlank(requestBody, "{}"));
        } catch (Exception ex) {
            throw new BusinessException(
                    DistributedErrorCode.INVALID_JSON_REQUEST.getCode(),
                    DistributedErrorCode.INVALID_JSON_REQUEST.getMessage()
            );
        }
    }

    private String readRequiredText(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        String value = node.isMissingNode() || node.isNull() ? null : StringUtils.trimToNull(node.asText());
        if (value == null) {
            throw new BusinessException(
                    DistributedErrorCode.MODEL_REQUIRED.getCode(),
                    DistributedErrorCode.MODEL_REQUIRED.getMessage()
            );
        }
        return value;
    }

    private Boolean readBoolean(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asBoolean();
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
