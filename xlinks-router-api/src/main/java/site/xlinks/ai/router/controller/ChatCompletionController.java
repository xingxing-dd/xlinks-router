package site.xlinks.ai.router.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.controller.strategy.ChatCompletionResponseStrategy;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.interceptor.BearerTokenInterceptor;
import site.xlinks.ai.router.service.ChatService;

import java.util.List;

/**
 * 对外 API - Chat Completions
 *
 * 入口层：接收标准 OpenAI 请求 DTO
 * - 鉴权已由 {@link BearerTokenInterceptor} 统一处理
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "大模型对话接口")
public class ChatCompletionController {

    private final ChatService chatService;
    private final List<ChatCompletionResponseStrategy<?>> responseStrategies;
    private final ObjectMapper objectMapper;

    @PostMapping("/{endpoint}/chat/completions")
    @Operation(summary = "Chat Completions",
            description = "发送对话请求，返回模型生成的文本。兼容 OpenAI API 格式。")
    public Object chatCompletions(
            HttpServletRequest servletRequest,
            @PathVariable(value = "endpoint") String endpoint,
            @RequestBody String requestBody) throws JsonProcessingException {
        ChatCompletionRequest request = objectMapper.readValue(requestBody, ChatCompletionRequest.class);
        log.info("Received chat completion request,{} model: {}", endpoint, request.getModel());
        request.setRequestBody(requestBody);
        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        for (ChatCompletionResponseStrategy<?> strategy : responseStrategies) {
            if (strategy.supports(request)) {
                return strategy.handle(token, endpoint, request);
            }
        }
        return Result.error(500, "未找到匹配的响应策略");
    }

    @GetMapping("/models")
    @Operation(summary = "Models List", description = "获取可用的模型列表")
    public Result<Object> modelsList(
            HttpServletRequest servletRequest,
            @Parameter(description = "Bearer Token", required = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        String token = (String) servletRequest.getAttribute(BearerTokenInterceptor.ATTR_BEARER_TOKEN);

        try {
            return Result.success(chatService.listModels(token));
        } catch (BusinessException e) {
            log.warn("models list error: {}", e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error", e);
            return Result.error(500, "服务器内部错误");
        }
    }
}
