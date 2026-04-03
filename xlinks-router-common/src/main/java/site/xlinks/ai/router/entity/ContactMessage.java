package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系我们消息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact_messages")
public class ContactMessage extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提单用户ID，用于前台按当前账号查询历史问题。
     */
    private Long accountId;

    private String name;

    private String email;

    private String subject;

    private String message;

    /**
     * 处理状态：0-未处理，1-已处理。
     */
    private Integer status;
}