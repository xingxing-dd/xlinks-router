package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Customer Model Mapping 创建 DTO
 */
@Data
@Schema(description = "Customer Model Mapping 创建请求")
public class CustomerModelMappingCreateDTO {

    @Schema(description = "Customer Model ID", required = true)
    @NotNull(message = "Customer Model ID 不能为空")
    private Long customerModelId;

    @Schema(description = "Provider Model ID", required = true)
    @NotNull(message = "Provider Model ID 不能为空")
    private Long providerModelId;

    @Schema(description = "优先级，值越小优先级越高")
    private Integer priority = 100;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
