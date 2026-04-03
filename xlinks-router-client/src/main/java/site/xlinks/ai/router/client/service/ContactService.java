package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.contact.ContactSubmitRequest;
import site.xlinks.ai.router.client.vo.ContactChannelConfigVO;
import site.xlinks.ai.router.client.vo.ContactFaqVO;
import site.xlinks.ai.router.client.vo.ContactHistoryItemVO;
import site.xlinks.ai.router.client.vo.ContactRecordVO;
import site.xlinks.ai.router.client.vo.ContactSubjectOptionVO;
import site.xlinks.ai.router.common.enums.ContactSubjectEnum;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ContactFaq;
import site.xlinks.ai.router.entity.ContactMessage;
import site.xlinks.ai.router.entity.ContactMessageRecord;
import site.xlinks.ai.router.entity.ContactChannelConfig;
import site.xlinks.ai.router.mapper.ContactFaqMapper;
import site.xlinks.ai.router.mapper.ContactChannelConfigMapper;
import site.xlinks.ai.router.mapper.ContactMessageMapper;
import site.xlinks.ai.router.mapper.ContactMessageRecordMapper;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactService {

    private final ContactChannelConfigMapper contactChannelConfigMapper;
    private final ContactFaqMapper contactFaqMapper;
    private final ContactMessageMapper contactMessageMapper;
    private final ContactMessageRecordMapper contactMessageRecordMapper;

    /**
     * 返回前端联系表单的主题下拉选项。
     */
    public List<ContactSubjectOptionVO> listSubjects() {
        return Arrays.stream(ContactSubjectEnum.values())
                .map(item -> new ContactSubjectOptionVO(item.getCode(), item.getDescription()))
                .toList();
    }

    /**
     * 返回前台联系页的联系方式配置。
     */
    public List<ContactChannelConfigVO> listChannelConfigs() {
        return contactChannelConfigMapper.selectList(
                        new LambdaQueryWrapper<ContactChannelConfig>()
                                .eq(ContactChannelConfig::getStatus, 1)
                                .orderByAsc(ContactChannelConfig::getSortOrder)
                                .orderByAsc(ContactChannelConfig::getId)
                ).stream()
                .map(this::toChannelConfigVO)
                .toList();
    }

    /**
     * 返回联系页 FAQ 配置。
     */
    public List<ContactFaqVO> listFaqs() {
        return contactFaqMapper.selectList(
                        new LambdaQueryWrapper<ContactFaq>()
                                .eq(ContactFaq::getStatus, 1)
                                .orderByAsc(ContactFaq::getSortOrder)
                                .orderByAsc(ContactFaq::getId)
                ).stream()
                .map(this::toFaqVO)
                .toList();
    }

    /**
     * 校验并保存用户提交的联系消息。
     */
    public void submit(ContactSubmitRequest request) {
        if (!ContactSubjectEnum.contains(request.getSubject())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系主题不合法");
        }

        var account = CustomerAccountContext.requireAccount();
        ContactMessage entity = new ContactMessage();
        entity.setAccountId(account.getId());
        entity.setName(normalize(request.getName()));
        entity.setEmail(normalize(request.getEmail()));
        entity.setSubject(normalize(request.getSubject()));
        entity.setMessage(normalize(request.getMessage()));
        entity.setStatus(0);
        contactMessageMapper.insert(entity);

        ContactMessageRecord initialRecord = new ContactMessageRecord();
        initialRecord.setContactMessageId(entity.getId());
        initialRecord.setSenderType("user");
        initialRecord.setSenderName(account.getUsername());
        initialRecord.setContent(entity.getMessage());
        contactMessageRecordMapper.insert(initialRecord);
    }

    /**
     * 查询当前登录账号提交过的问题历史。
     */
    public List<ContactHistoryItemVO> listCurrentAccountHistory() {
        Long accountId = CustomerAccountContext.requireAccount().getId();
        return contactMessageMapper.selectList(
                        new LambdaQueryWrapper<ContactMessage>()
                                .eq(ContactMessage::getAccountId, accountId)
                                .orderByDesc(ContactMessage::getCreatedAt)
                ).stream()
                .map(this::toHistoryItem)
                .toList();
    }

    /**
     * 查询当前账号某个问题下的全部沟通记录。
     */
    public List<ContactRecordVO> listCurrentAccountRecords(Long contactMessageId) {
        ContactMessage message = getCurrentAccountMessage(contactMessageId);
        return contactMessageRecordMapper.selectList(
                        new LambdaQueryWrapper<ContactMessageRecord>()
                                .eq(ContactMessageRecord::getContactMessageId, message.getId())
                                .orderByAsc(ContactMessageRecord::getCreatedAt)
                ).stream()
                .map(this::toRecordVO)
                .toList();
    }

    /**
     * 统一去除首尾空格，并在空值时抛出业务异常。
     */
    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系内容不能为空");
        }
        return value.trim();
    }

    private ContactHistoryItemVO toHistoryItem(ContactMessage entity) {
        ContactHistoryItemVO item = new ContactHistoryItemVO();
        item.setId(entity.getId());
        item.setSubject(entity.getSubject());
        item.setSubjectLabel(resolveSubjectLabel(entity.getSubject()));
        item.setMessage(entity.getMessage());
        item.setStatus(entity.getStatus());
        item.setCreatedAt(entity.getCreatedAt());
        return item;
    }

    private ContactRecordVO toRecordVO(ContactMessageRecord entity) {
        ContactRecordVO vo = new ContactRecordVO();
        vo.setId(entity.getId());
        vo.setSenderType(entity.getSenderType());
        vo.setSenderName(entity.getSenderName());
        vo.setContent(entity.getContent());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private ContactChannelConfigVO toChannelConfigVO(ContactChannelConfig entity) {
        ContactChannelConfigVO vo = new ContactChannelConfigVO();
        vo.setId(entity.getId());
        vo.setChannelType(entity.getChannelType());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setContactValue(entity.getContactValue());
        vo.setActionLink(entity.getActionLink());
        vo.setActionLabel(entity.getActionLabel());
        return vo;
    }

    private ContactFaqVO toFaqVO(ContactFaq entity) {
        ContactFaqVO vo = new ContactFaqVO();
        vo.setId(entity.getId());
        vo.setQuestion(entity.getQuestion());
        vo.setAnswer(entity.getAnswer());
        return vo;
    }

    private ContactMessage getCurrentAccountMessage(Long contactMessageId) {
        Long accountId = CustomerAccountContext.requireAccount().getId();
        ContactMessage message = contactMessageMapper.selectOne(
                new LambdaQueryWrapper<ContactMessage>()
                        .eq(ContactMessage::getId, contactMessageId)
                        .eq(ContactMessage::getAccountId, accountId)
        );
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系问题不存在");
        }
        return message;
    }

    private String resolveSubjectLabel(String subjectCode) {
        for (ContactSubjectEnum item : ContactSubjectEnum.values()) {
            if (item.getCode().equals(subjectCode)) {
                return item.getDescription();
            }
        }
        return subjectCode;
    }
}