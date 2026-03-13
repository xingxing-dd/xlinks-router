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
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
