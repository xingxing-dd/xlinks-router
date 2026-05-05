package site.xlinks.ai.router.support.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal")
public class HealthController {

    @Value("${spring.application.name:xlinks-router-api}")
    private String applicationName;

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("application", applicationName);
        payload.put("status", "UP");
        payload.put("timestamp", OffsetDateTime.now().toString());
        return Result.success(payload);
    }
}
