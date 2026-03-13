package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Chat Completion 响应 DTO
 */
@Data
@Schema(description = "Chat Completion 响应")
public class ChatCompletionResponse {

    @Schema(description = "请求 ID")
    private String id;

    @Schema(description = "对象类型")
    private String object;

    @Schema(description = "创建时间戳")
    private Long created;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "选择列表")
    private List<Choice> choices;

    @Schema(description = "使用统计")
    private Usage usage;

    /**
     * 选择
     */
    @Data
    @Schema(description = "选择")
    public static class Choice {
        
        @Schema(description = "索引")
        private Integer index;
        
        @Schema(description = "消息")
        private Message message;
        
        @Schema(description = "完成原因")
        private String finishReason;
    }

    /**
     * 消息
     */
    @Data
    @Schema(description = "消息")
    public static class Message {
        
        @Schema(description = "角色")
        private String role;
        
        @Schema(description = "内容")
        private String content;
    }

    /**
     * 使用统计
     */
    @Data
    @Schema(description = "使用统计")
    public static class Usage {
        
        @Schema(description = "提示词 token 数")
        private Integer promptTokens;
        
        @Schema(description = "补全 token 数")
        private Integer completionTokens;
        
        @Schema(description = "总 token 数")
        private Integer totalTokens;
    }
}
