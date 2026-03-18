package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Customer Model 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_models")
public class CustomerModel extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台逻辑模型编码，对外暴露
     */
    private String logicModelCode;

    /**
     * 逻辑模型名称
     */
    private String logicModelName;

    /**
     * 模型类型：chat、embedding、image 等
     */
    private String modelType;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 是否默认模型：1-是，0-否
     */
    private Integer isDefault;

    /**
     * 备注
     */
    private String remark;
}
