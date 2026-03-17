package site.xlinks.ai.router.client.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.model.AvailableModelItemResponse;
import site.xlinks.ai.router.client.dto.model.CustomerModelItemResponse;
import site.xlinks.ai.router.client.dto.model.ModelDetailResponse;
import site.xlinks.ai.router.client.dto.model.ModelRouteItemResponse;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ModelController {

    @GetMapping("/customer-models")
    public Result<PageResult<CustomerModelItemResponse>> getCustomerModels(@RequestParam(defaultValue = "1") Integer page,
                                                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        List<CustomerModelItemResponse> records = List.of(
                new CustomerModelItemResponse(1L, "claude-sonnet", "Claude Sonnet", "chat", 1, 1, "主力模型", "2026-03-08 10:00:00"),
                new CustomerModelItemResponse(2L, "claude-haiku", "Claude Haiku", "chat", 1, 0, "快速响应模型", "2026-03-10 14:20:00")
        );
        return Result.success(PageResult.of(records, records.size(), page, pageSize));
    }

    @GetMapping("/models/available")
    public Result<List<AvailableModelItemResponse>> getAvailableModels() {
        return Result.success(List.of(
                new AvailableModelItemResponse(1L, "claude-3-7-sonnet", "Anthropic", "高性能对话模型，适合复杂推理任务", "$3.00/M", "$15.00/M", "200K", "available"),
                new AvailableModelItemResponse(2L, "claude-haiku-4-5", "Anthropic", "快速响应，高性价比的轻量级模型", "$1.00/M", "$5.00/M", "200K", "available"),
                new AvailableModelItemResponse(3L, "claude-opus-4", "Anthropic", "最强大的推理模型，适合复杂任务", "$15.00/M", "$75.00/M", "200K", "limited")
        ));
    }

    @GetMapping("/models/{id}")
    public Result<ModelDetailResponse> getModelDetail(@PathVariable Long id) {
        ModelDetailResponse response = new ModelDetailResponse();
        response.setId(id);
        response.setName("claude-3-7-sonnet");
        response.setProvider("Anthropic");
        response.setDescription("高性能对话模型");
        response.setInputPrice("$3.00/M");
        response.setOutputPrice("$15.00/M");
        response.setContextWindow("200K");
        response.setRoutes(List.of(
                new ModelRouteItemResponse(1L, "OpenAI Compatible", "claude-3-7-sonnet-20250219", 1),
                new ModelRouteItemResponse(2L, "Anthropic Official", "claude-3-7-sonnet", 2)
        ));
        return Result.success(response);
    }
}
