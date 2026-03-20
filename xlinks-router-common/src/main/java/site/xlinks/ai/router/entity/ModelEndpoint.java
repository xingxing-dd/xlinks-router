package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模型端点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("model_endpoints")
public class ModelEndpoint extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String endpointName;

    private String endpointDesc;

    private String endpointUrl;

    private Integer status;

    @TableLogic
    private Integer deleted;
}
