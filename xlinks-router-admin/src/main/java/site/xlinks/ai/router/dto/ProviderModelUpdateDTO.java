package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Provider Model 更新 DTO
 */
@Data
@Schema(description = "Provider Model 更新请求")
public class ProviderModelUpdateDTO {

    @Schema(description = "Provider Model 名称")
    private String providerModelName;

    @Schema(description = "备注")
    private String remark;
}
