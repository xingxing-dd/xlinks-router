package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Chat Completion 请求 DTO
 */
@Data
@Schema(description = "Chat Completion 请求")
public class ChatCompletionRequest {

    @Schema(description = "模型名称", required = true)
    private String model;

    @Schema(description = "消息列表", required = true)
    private List<ChatMessage> messages;

    @Schema(description = "采样温度")
    private Double temperature;

    @Schema(description = "最大 token 数")
    private Integer maxTokens;

    @Schema(description = "是否流式返回")
    private Boolean stream;

    @Schema(description = "nucleus 采样")
    private Double topP;

    @Schema(description = "频率惩罚")
    private Double frequencyPenalty;

    @Schema(description = "存在惩罚")
    private Double presencePenalty;

    @Schema(description = "停止符")
    private List<String> stop;

    @Schema(description = "额外参数")
    private Map<String, Object> extraParams;

    /**
     * 消息
     */
    @Data
    @Schema(description = "消息")
    public static class ChatMessage {
        
        @Schema(description = "角色：system、user、assistant", required = true)
        private String role;

        @Schema(description = "消息内容", required = true)
        private String content;
    }
}
