package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.service.SubscriptionService;
import site.xlinks.ai.router.vo.SubscriptionRecordVO;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscription Management", description = "Plan subscription record APIs")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    @Operation(summary = "Subscription list")
    public Result<PageResult<SubscriptionRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String accountKeyword,
                                                         @RequestParam(required = false) Long planId,
                                                         @RequestParam(required = false) Integer status,
                                                         @RequestParam(required = false) String source) {
        var pageResult = subscriptionService.pageQuery(page, pageSize, accountKeyword, planId, status, source);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Subscription detail")
    public Result<SubscriptionRecordVO> get(@PathVariable Long id) {
        return Result.success(subscriptionService.getDetail(id));
    }
}
