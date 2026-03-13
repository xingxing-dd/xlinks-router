package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Customer Model 更新 DTO
 */
@Data
@Schema(description = "Customer Model 更新请求")
public class CustomerModelUpdateDTO {

    @Schema(description = "逻辑模型名称")
    private String logicModelName;

    @Schema(description = "是否默认模型")
    private Integer isDefault;

    @Schema(description = "备注")
    private String remark;
}
