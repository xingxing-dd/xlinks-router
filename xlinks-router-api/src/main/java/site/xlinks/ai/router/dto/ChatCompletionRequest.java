package site.xlinks.ai.router.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
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

    @Schema(description = "是否流式返回")
    private Boolean stream;


    private String requestBody;

}
