package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Provider 创建 DTO
 */
@Data
@Schema(description = "Provider 创建请求")
public class ProviderCreateDTO {

    @Schema(description = "提供商编码", required = true)
    @NotBlank(message = "提供商编码不能为空")
    private String providerCode;

    @Schema(description = "提供商名称", required = true)
    @NotBlank(message = "提供商名称不能为空")
    private String providerName;

    @Schema(description = "协议类型", required = true)
    @NotBlank(message = "协议类型不能为空")
    private String providerType;

    @Schema(description = "基础请求 URL", required = true)
    @NotBlank(message = "基础请求 URL 不能为空")
    private String baseUrl;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "备注")
    private String remark;
}
