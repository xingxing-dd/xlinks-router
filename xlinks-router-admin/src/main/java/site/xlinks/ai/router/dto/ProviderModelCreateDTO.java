package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 * Provider Model 创建 DTO
 */
@Data
@Schema(description = "Provider Model 创建请求")
public class ProviderModelCreateDTO {

    @Schema(description = "Provider ID", required = true)
    @NotNull(message = "Provider ID 不能为空")
    private Long providerId;

    @Schema(description = "Provider Model 编码", required = true)
    @NotBlank(message = "Provider Model 编码不能为空")
    private String providerModelCode;

    @Schema(description = "Provider Model 名称", required = true)
    @NotBlank(message = "Provider Model 名称不能为空")
    private String providerModelName;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
