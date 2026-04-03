package site.xlinks.ai.router.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.context.AdminAccountContext;
import site.xlinks.ai.router.dto.ContactReplyDTO;
import site.xlinks.ai.router.common.result.PageResult;
import site.xlinks.ai.router.common.result.Result;
import site.xlinks.ai.router.service.ContactMessageService;
import site.xlinks.ai.router.vo.ContactRecordVO;
import site.xlinks.ai.router.vo.ContactMessageVO;

import java.util.List;

@RestController
@RequestMapping("/admin/contact-messages")
@RequiredArgsConstructor
@Tag(name = "Contact Message Management", description = "Contact message management APIs")
public class ContactMessageController {

    private final ContactMessageService contactMessageService;

    /**
     * 分页返回联系消息列表，供管理后台检索与查看。
     */
    @GetMapping
    @Operation(summary = "Contact message list")
    public Result<PageResult<ContactMessageVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer status) {
        var pageResult = contactMessageService.pageQuery(page, pageSize, keyword, status);
        List<ContactMessageVO> records = pageResult.getRecords().stream().map(entity -> {
            ContactMessageVO vo = new ContactMessageVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        }).toList();
        return Result.success(PageResult.of(records, pageResult.getTotal(), (int) pageResult.getCurrent(), (int) pageResult.getSize()));
    }

    /**
     * 查询指定问题的沟通记录详情。
     */
    @GetMapping("/{id}/records")
    @Operation(summary = "Contact message records")
    public Result<List<ContactRecordVO>> records(@PathVariable Long id) {
        return Result.success(contactMessageService.listRecords(id));
    }

    /**
     * 管理员回复联系问题，并将状态更新为已处理。
     */
    @PostMapping("/{id}/reply")
    @Operation(summary = "Reply contact message")
    public Result<Void> reply(@PathVariable Long id, @Valid @RequestBody ContactReplyDTO request) {
        String adminName = AdminAccountContext.requireAccount().getUsername();
        contactMessageService.reply(id, request.getContent(), adminName);
        return Result.success();
    }
}