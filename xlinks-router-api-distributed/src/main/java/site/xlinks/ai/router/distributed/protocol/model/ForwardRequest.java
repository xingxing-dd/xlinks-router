package site.xlinks.ai.router.distributed.protocol.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ForwardRequest {

    /**
     * 协议类型，仅用于路由和转发策略判定。
     */
    private ForwardProtocol protocol;

    /**
     * 从原始请求体中提取的模型标识。
     */
    private String model;

    /**
     * 从原始请求体中提取的流式标记。
     */
    private Boolean stream;

    /**
     * 调用方 customer token。
     */
    private String customerToken;

    private CustomerTokenSource tokenSource;

    /**
     * 仅用于提取关键路由字段的轻量 JSON 视图，不作为最终转发报文来源。
     */
    private JsonNode payload;

    /**
     * 原始请求报文，后续真实转发阶段应优先使用该字段透传给 provider。
     */
    private String requestBody;

    private Map<String, String> passthroughHeaders;

    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }
}
