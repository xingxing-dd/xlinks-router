package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Customer Model Mapping 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_mapping")
public class CustomerModelMapping extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Customer Model ID
     */
    private Long customerModelId;

    /**
     * Provider Model ID
     */
    private Long providerModelId;

    /**
     * 优先级，值越小优先级越高
     */
    private Integer priority;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
