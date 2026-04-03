package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ContactMessage;
import site.xlinks.ai.router.entity.ContactMessageRecord;
import site.xlinks.ai.router.mapper.ContactMessageMapper;
import site.xlinks.ai.router.mapper.ContactMessageRecordMapper;
import site.xlinks.ai.router.vo.ContactRecordVO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactMessageService extends ServiceImpl<ContactMessageMapper, ContactMessage> {

    private final ContactMessageRecordMapper contactMessageRecordMapper;

    /**
     * 按关键词和处理状态分页查询联系消息，便于后台列表筛选。
     */
    public IPage<ContactMessage> pageQuery(Integer page, Integer pageSize, String keyword, Integer status) {
        LambdaQueryWrapper<ContactMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, ContactMessage::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w.like(ContactMessage::getName, keyword)
                        .or().like(ContactMessage::getEmail, keyword)
                        .or().like(ContactMessage::getSubject, keyword))
                .orderByDesc(ContactMessage::getCreatedAt);
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    /**
     * 返回指定问题下的全部沟通记录，供后台详情查看。
     */
    public List<ContactRecordVO> listRecords(Long contactMessageId) {
        getMessageOrThrow(contactMessageId);
        return contactMessageRecordMapper.selectList(
                        new LambdaQueryWrapper<ContactMessageRecord>()
                                .eq(ContactMessageRecord::getContactMessageId, contactMessageId)
                                .orderByAsc(ContactMessageRecord::getCreatedAt)
                ).stream()
                .map(this::toRecordVO)
                .toList();
    }

    /**
     * 写入管理员回复，并同步更新问题状态为已处理。
     */
    public void reply(Long contactMessageId, String content, String adminName) {
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "回复内容不能为空");
        }

        ContactMessage message = getMessageOrThrow(contactMessageId);
        ContactMessageRecord record = new ContactMessageRecord();
        record.setContactMessageId(contactMessageId);
        record.setSenderType("admin");
        record.setSenderName(adminName);
        record.setContent(content.trim());
        contactMessageRecordMapper.insert(record);

        ContactMessage update = new ContactMessage();
        update.setId(message.getId());
        update.setStatus(1);
        this.updateById(update);
    }

    private ContactMessage getMessageOrThrow(Long contactMessageId) {
        ContactMessage message = this.getById(contactMessageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系问题不存在");
        }
        return message;
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
}