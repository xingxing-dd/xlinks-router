package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Payment method configuration.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_methods")
public class PaymentMethod extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String methodCode;

    private String methodName;

    private String methodType;

    private String iconUrl;

    private Integer sort;

    private Integer status;

    private String configJson;

    private String remark;
}
