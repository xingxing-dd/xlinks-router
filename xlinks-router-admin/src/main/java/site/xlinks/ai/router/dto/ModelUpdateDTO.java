package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model 更新 DTO
 */
@Data
@Schema(description = "Model 更新请求")
public class ModelUpdateDTO {

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "模型描述")
    private String modelDesc;

    @Schema(description = "输入价格，单位：每百万 token")
    private BigDecimal inputPrice;

    @Schema(description = "输出价格，单位：每百万 token")
    private BigDecimal outputPrice;

    @Schema(description = "上下文大小，单位：K")
    private Integer contextSize;

    @Schema(description = "备注")
    private String remark;
}
