package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Customer Token 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_tokens")
public class CustomerToken extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * Token 值（SHA256 哈希存储）
     */
    private String tokenValue;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 允许访问的模型列表（JSON）
     */
    private String allowedModels;

    /**
     * 备注
     */
    private String remark;
}
