package site.xlinks.ai.router.distributed.protocol.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.distributed.protocol.model.DistributedErrorCode;

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
}
