package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Provider 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("providers")
public class Provider extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提供商编码，唯一标识
     */
    private String providerCode;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 协议类型
     */
    private String providerType;

    /**
     * 基础请求 URL
     */
    private String baseUrl;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
