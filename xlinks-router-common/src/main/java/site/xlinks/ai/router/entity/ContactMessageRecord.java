package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系问题沟通记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact_message_records")
public class ContactMessageRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联 contact_messages.id。
     */
    private Long contactMessageId;

    /**
     * 消息发送方：user/admin/system。
     */
    private String senderType;

    /**
     * 发送方名称。
     */
    private String senderName;

    /**
     * 本次沟通内容。
     */
    private String content;

}