package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Provider-specific model mapping.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("provider_models")
public class ProviderModel extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * Provider ID.
     */
    private Long providerId;

    /**
     * Standard model ID.
     */
    private Long modelId;

    /**
     * Upstream provider model code.
     */
    private String providerModelCode;

    /**
     * Upstream provider model name.
     */
    private String providerModelName;

    /**
     * Status: 1-enabled, 0-disabled.
     */
    private Integer status;

    @TableLogic
    private Integer deleted;

    private String remark;
}
