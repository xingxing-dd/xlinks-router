package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.service.UsageRecordService;
import site.xlinks.ai.router.vo.UsageRecordAccountSummaryVO;
import site.xlinks.ai.router.vo.UsageRecordFlowVO;
import site.xlinks.ai.router.vo.UsageRecordModelSummaryVO;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/usage-records")
@RequiredArgsConstructor
@Tag(name = "Usage Record Management", description = "Token usage record query APIs")
public class UsageRecordController {

    private final UsageRecordService usageRecordService;

    @GetMapping
    @Operation(summary = "Usage record flow list")
    public Result<PageResult<UsageRecordFlowVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String accountKeyword,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String usageType,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt) {

        var pageResult = usageRecordService.pageFlowQuery(
                page,
                pageSize,
                accountKeyword,
                modelCode,
                providerCode,
                usageType,
                requestId,
                responseStatus,
                startAt,
                endAt
        );
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/summary/account")
    @Operation(summary = "Usage summary grouped by account")
    public Result<PageResult<UsageRecordAccountSummaryVO>> accountSummary(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String accountKeyword,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String usageType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt) {

        var pageResult = usageRecordService.pageAccountSummary(
                page,
                pageSize,
                accountKeyword,
                modelCode,
                providerCode,
                usageType,
                startAt,
                endAt
        );
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/summary/model")
    @Operation(summary = "Usage summary grouped by model")
    public Result<PageResult<UsageRecordModelSummaryVO>> modelSummary(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String accountKeyword,
            @RequestParam(required = false) String modelCode,
            @RequestParam(required = false) String providerCode,
            @RequestParam(required = false) String usageType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt) {

        var pageResult = usageRecordService.pageModelSummary(
                page,
                pageSize,
                accountKeyword,
                modelCode,
                providerCode,
                usageType,
                startAt,
                endAt
        );
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }
}
