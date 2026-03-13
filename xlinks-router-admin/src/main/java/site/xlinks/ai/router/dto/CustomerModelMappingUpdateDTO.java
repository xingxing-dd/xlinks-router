package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Customer Model Mapping 更新 DTO
 */
@Data
@Schema(description = "Customer Model Mapping 更新请求")
public class CustomerModelMappingUpdateDTO {

    @Schema(description = "优先级")
    private Integer priority;

    @Schema(description = "备注")
    private String remark;
}
