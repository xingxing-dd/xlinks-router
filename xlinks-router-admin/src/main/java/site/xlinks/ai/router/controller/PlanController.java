package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.dto.PlanCreateDTO;
import site.xlinks.ai.router.dto.PlanUpdateDTO;
import site.xlinks.ai.router.service.PlanService;
import site.xlinks.ai.router.vo.PlanVO;

/**
 * Plan management API.
 */
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@Tag(name = "Plan Management", description = "Plan and payment link management APIs")
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @Operation(summary = "Create plan")
    public Result<PlanVO> create(@Valid @RequestBody PlanCreateDTO dto) {
        return Result.success(planService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Plan list")
    public Result<PageResult<PlanVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String planName,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Integer visible,
                                           @RequestParam(required = false) Long accountId) {
        var pageResult = planService.pageQuery(page, pageSize, planName, status, visible, accountId);
        return Result.success(PageResult.of(
                pageResult.getRecords(),
                pageResult.getTotal(),
                (int) pageResult.getCurrent(),
                (int) pageResult.getSize()
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Plan detail")
    public Result<PlanVO> get(@PathVariable Long id) {
        return Result.success(planService.getDetail(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update plan")
    public Result<PlanVO> update(@PathVariable Long id, @Valid @RequestBody PlanUpdateDTO dto) {
        return Result.success(planService.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Enable or disable plan")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        planService.updateStatus(id, status);
        return Result.success();
    }

    @PatchMapping("/{id}/visible")
    @Operation(summary = "Update plan visibility")
    public Result<Void> updateVisible(@PathVariable Long id, @RequestParam Integer visible) {
        planService.updateVisible(id, visible);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete plan")
    public Result<Void> delete(@PathVariable Long id) {
        planService.deleteById(id);
        return Result.success();
    }
}
