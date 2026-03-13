package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Provider Token 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("provider_tokens")
public class ProviderToken extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * Token 名称
     */
    private String tokenName;

    /**
     * Token 值（加密存储）
     */
    private String tokenValue;

    /**
     * Token 状态：1-正常，0-禁用
     */
    private Integer tokenStatus;

    /**
     * 配额总量
     */
    private Long quotaTotal;

    /**
     * 已使用配额
     */
    private Long quotaUsed;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 最后使用时间
     */
    private LocalDateTime lastUsedAt;

    /**
     * 备注
     */
    private String remark;
}
