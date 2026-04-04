package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 统一模型实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("models")
public class Model extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String modelName;

    private String modelCode;

    private String modelDesc;

    private BigDecimal inputPrice;

    private BigDecimal outputPrice;

    private Integer contextSize;

    private Integer status;

    @TableLogic
    private Integer deleted;

    private String remark;
}
