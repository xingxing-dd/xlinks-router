package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Customer token entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_tokens")
public class CustomerToken extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private String customerName;

    private String tokenName;

    /**
     * Stored as SHA-256 hash.
     */
    private String tokenValue;

    private Integer status;

    private LocalDateTime expireTime;

    /**
     * JSON array string of allowed models.
     */
    private String allowedModels;

    /**
     * Daily usage quota. NULL means unlimited.
     */
    private BigDecimal dailyQuota;

    /**
     * Used quota for the current day snapshot.
     */
    private BigDecimal usedQuota;

    /**
     * Total available quota. NULL means unlimited.
     */
    private BigDecimal totalQuota;

    /**
     * Total used quota.
     */
    private BigDecimal totalUsedQuota;

    /**
     * Total tokens consumed by this token today.
     */
    @TableField(exist = false)
    private Long todayUsedTokens;

    /**
     * Total tokens consumed by this token across all time.
     */
    @TableField(exist = false)
    private Long totalUsedTokens;

    private String remark;
}
