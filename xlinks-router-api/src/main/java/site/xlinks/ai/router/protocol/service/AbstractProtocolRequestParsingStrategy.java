package site.xlinks.ai.router.protocol.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import org.apache.commons.lang3.StringUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.protocol.model.DistributedErrorCode;

abstract class AbstractProtocolRequestParsingStrategy implements ProtocolRequestParsingStrategy {

    protected final ObjectMapper objectMapper;

    protected AbstractProtocolRequestParsingStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected JsonNode parseJson(String requestBody) {
        try {
            return objectMapper.readTree(StringUtils.defaultIfBlank(requestBody, "{}"));
        } catch (Exception ex) {
            throw new BusinessException(
                    DistributedErrorCode.INVALID_JSON_REQUEST.getCode(),
                    DistributedErrorCode.INVALID_JSON_REQUEST.getMessage()
            );
        }
    }

    protected ParsedRequestFields parseRequestFields(String requestBody) {
        try (JsonParser parser = objectMapper.getFactory().createParser(StringUtils.defaultIfBlank(requestBody, "{}"))) {
            JsonToken firstToken = parser.nextToken();
            if (firstToken == null) {
                return new ParsedRequestFields(null, null);
            }
            if (firstToken != JsonToken.START_OBJECT) {
                throw invalidJsonRequest();
            }

            String model = null;
            Boolean stream = null;
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    throw invalidJsonRequest();
                }
                String fieldName = parser.currentName();
                JsonToken valueToken = parser.nextToken();
                if ("model".equals(fieldName)) {
                    model = readTextValue(parser, valueToken);
                    if (stream != null) {
                        return new ParsedRequestFields(model, stream);
                    }
                    continue;
                }
                if ("stream".equals(fieldName)) {
                    stream = readBooleanValue(parser, valueToken);
                    if (model != null) {
                        return new ParsedRequestFields(model, stream);
                    }
                    continue;
                }
                if (valueToken != null && valueToken.isStructStart()) {
                    parser.skipChildren();
                }
            }
            return new ParsedRequestFields(model, stream);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidJsonRequest();
        }
    }

    protected String readRequiredText(JsonNode payload, String fieldName) {
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

    protected Boolean readBoolean(JsonNode payload, String fieldName) {
        JsonNode node = payload.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return node.asBoolean();
    }

    protected String readRequiredText(ParsedRequestFields fields) {
        if (fields == null || fields.model() == null) {
            throw new BusinessException(
                    DistributedErrorCode.MODEL_REQUIRED.getCode(),
                    DistributedErrorCode.MODEL_REQUIRED.getMessage()
            );
        }
        return fields.model();
    }

    protected Boolean readBoolean(ParsedRequestFields fields) {
        return fields == null ? null : fields.stream();
    }

    private String readTextValue(JsonParser parser, JsonToken token) throws java.io.IOException {
        if (token == null || token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token.isScalarValue()) {
            return StringUtils.trimToNull(parser.getValueAsString());
        }
        parser.skipChildren();
        return null;
    }

    private Boolean readBooleanValue(JsonParser parser, JsonToken token) throws java.io.IOException {
        if (token == null || token == JsonToken.VALUE_NULL) {
            return null;
        }
        if (token.isScalarValue()) {
            return parser.getValueAsBoolean();
        }
        parser.skipChildren();
        return null;
    }

    private BusinessException invalidJsonRequest() {
        return new BusinessException(
                DistributedErrorCode.INVALID_JSON_REQUEST.getCode(),
                DistributedErrorCode.INVALID_JSON_REQUEST.getMessage()
        );
    }

    protected record ParsedRequestFields(String model, Boolean stream) {
    }
}
