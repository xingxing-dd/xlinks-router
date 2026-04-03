package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 联系方式渠道配置，支持同类型多条联系方式动态展示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contact_channel_configs")
public class ContactChannelConfig extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 渠道类型：email / online / phone。
     */
    private String channelType;

    /**
     * 渠道标题，如“邮箱支持”。
     */
    private String title;

    /**
     * 渠道描述。
     */
    private String description;

    /**
     * 展示联系方式，支持邮箱、电话、链接等。
     */
    private String contactValue;

    /**
     * 点击跳转链接，可为空；为空时前端按类型兜底生成。
     */
    private String actionLink;

    /**
     * 操作按钮/链接文案。
     */
    private String actionLabel;

    /**
     * 排序值，越小越靠前。
     */
    private Integer sortOrder;

    /**
     * 状态：1-启用，0-禁用。
     */
    private Integer status;
}