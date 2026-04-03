package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系页常见问题实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact_faqs")
public class ContactFaq extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 问题标题。
     */
    private String question;

    /**
     * 问题答案。
     */
    private String answer;

    /**
     * 排序值，越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-禁用。
     */
    private Integer status;
}