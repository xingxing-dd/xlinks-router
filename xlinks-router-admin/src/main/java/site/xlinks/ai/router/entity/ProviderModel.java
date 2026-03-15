package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Provider Model 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("provider_models")
public class ProviderModel extends BaseEntity {

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
     * Provider Model 编码
     */
    private String providerModelCode;

    /**
     * Provider Model 名称
     */
    private String providerModelName;

    /**
     * 模型类型：chat、embedding、image 等
     */
    private String modelType;

    /**
     * 使用类型：0-不限制，1-仅套餐可用，2-仅余额可用
     */
    private Integer usageType;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
