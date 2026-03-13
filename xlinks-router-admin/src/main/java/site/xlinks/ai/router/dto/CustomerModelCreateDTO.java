package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Customer Model 创建 DTO
 */
@Data
@Schema(description = "Customer Model 创建请求")
public class CustomerModelCreateDTO {

    @Schema(description = "逻辑模型编码", required = true)
    @NotBlank(message = "逻辑模型编码不能为空")
    private String logicModelCode;

    @Schema(description = "逻辑模型名称", required = true)
    @NotBlank(message = "逻辑模型名称不能为空")
    private String logicModelName;

    @Schema(description = "模型类型", required = true)
    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "是否默认模型：1-是，0-否")
    private Integer isDefault = 0;

    @Schema(description = "备注")
    private String remark;
}
