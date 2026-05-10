package site.xlinks.ai.router.client.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.Result;

@RestController
@RequestMapping("/api/v1/public/stats")
@RequiredArgsConstructor
public class PublicStatsController {

    private static final String NEW_SITE_CLICK_COUNT_KEY = "xlinks:stats:new-site-click-count";

    private final StringRedisTemplate redisTemplate;

    @PostMapping("/new-site-click")
    public Result<Void> countNewSiteClick() {
        redisTemplate.opsForValue().increment(NEW_SITE_CLICK_COUNT_KEY);
        return Result.success();
    }
}
