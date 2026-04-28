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
import site.xlinks.ai.router.service.CustomerOrderService;
import site.xlinks.ai.router.vo.CustomerOrderVO;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/customer-orders")
@RequiredArgsConstructor
@Tag(name = "Customer Order Management", description = "Customer order query APIs")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;

    @GetMapping
    @Operation(summary = "Customer order list")
    public Result<PageResult<CustomerOrderVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String orderKeyword,
            @RequestParam(required = false) String accountKeyword,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) String paymentChannel,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAt) {

        var pageResult = customerOrderService.pageQuery(
                page,
                pageSize,
                orderKeyword,
                accountKeyword,
                orderType,
                paymentChannel,
                status,
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
