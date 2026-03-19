package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Model 创建 DTO
 */
@Data
@Schema(description = "Model 创建请求")
public class ModelCreateDTO {

    @Schema(description = "模型名称", required = true)
    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @Schema(description = "模型编码", required = true)
    @NotBlank(message = "模型编码不能为空")
    private String modelCode;

    @Schema(description = "模型端点 ID", required = true)
    @NotNull(message = "模型端点 ID 不能为空")
    private Long endpointId;

    @Schema(description = "提供商 ID", required = true)
    @NotNull(message = "提供商 ID 不能为空")
    private Long providerId;

    @Schema(description = "模型描述")
    private String modelDesc;

    @Schema(description = "输入价格，单位：每百万 token")
    private BigDecimal inputPrice;

    @Schema(description = "输出价格，单位：每百万 token")
    private BigDecimal outputPrice;

    @Schema(description = "上下文大小，单位：K")
    private Integer contextSize;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
