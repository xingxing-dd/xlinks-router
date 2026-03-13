package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.ChatCompletionRequest;
import site.xlinks.ai.router.dto.ChatCompletionResponse;
import site.xlinks.ai.router.service.ChatService;

/**
 * 对外 API - Chat Completions
 */
@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Tag(name = "Chat API", description = "大模型对话接口")
public class ChatCompletionController {

    private final ChatService chatService;

    @PostMapping("/chat/completions")
    @Operation(summary = "Chat Completions", description = "发送对话请求，返回模型生成的文本")
    public Result<ChatCompletionResponse> chatCompletions(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody ChatCompletionRequest request) {
        
        log.info("Received chat completion request, model: {}", request.getModel());
        
        // 验证 Authorization header
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(4002, "无效的 Authorization header");
        }
        
        String token = authorization.substring(7);
        ChatCompletionResponse response = chatService.chatCompletions(token, request);
        
        return Result.success(response);
    }

    @GetMapping("/models")
    @Operation(summary = "Models List", description = "获取可用的模型列表")
    public Result<Object> modelsList(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(4002, "无效的 Authorization header");
        }
        
        String token = authorization.substring(7);
        return Result.success(chatService.listModels(token));
    }
}
