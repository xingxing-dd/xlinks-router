package site.xlinks.ai.router.client.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.dto.contact.ContactSubmitRequest;
import site.xlinks.ai.router.client.vo.ContactHistoryItemVO;
import site.xlinks.ai.router.client.vo.ContactRecordVO;
import site.xlinks.ai.router.client.service.ContactService;
import site.xlinks.ai.router.client.vo.ContactChannelConfigVO;
import site.xlinks.ai.router.client.vo.ContactFaqVO;
import site.xlinks.ai.router.client.vo.ContactSubjectOptionVO;
import site.xlinks.ai.router.common.result.Result;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    /**
     * 提供联系表单主题下拉选项，供前端动态渲染。
     */
    @GetMapping("/contact/subjects")
    public Result<List<ContactSubjectOptionVO>> listSubjects() {
        return Result.success(contactService.listSubjects());
    }

    /**
     * 返回联系页联系方式配置。
     */
    @GetMapping("/contact/channels")
    public Result<List<ContactChannelConfigVO>> listChannels() {
        return Result.success(contactService.listChannelConfigs());
    }

    /**
     * 返回联系页常见问题配置。
     */
    @GetMapping("/contact/faqs")
    public Result<List<ContactFaqVO>> listFaqs() {
        return Result.success(contactService.listFaqs());
    }

    /**
     * 返回当前账号提交过的问题历史。
     */
    @GetMapping("/contact/history")
    public Result<List<ContactHistoryItemVO>> listHistory() {
        return Result.success(contactService.listCurrentAccountHistory());
    }

    /**
     * 返回当前账号某个问题下的沟通记录详情。
     */
    @GetMapping("/contact/history/{id}/records")
    public Result<List<ContactRecordVO>> listRecords(@PathVariable Long id) {
        return Result.success(contactService.listCurrentAccountRecords(id));
    }

    /**
     * 接收并保存用户提交的联系消息。
     */
    @PostMapping("/contact")
    public Result<Void> submitContact(@Valid @RequestBody ContactSubmitRequest request) {
        contactService.submit(request);
        return Result.success();
    }
}
