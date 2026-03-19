package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Provider 更新 DTO
 */
@Data
@Schema(description = "Provider 更新请求")
public class ProviderUpdateDTO {

    @Schema(description = "提供商名称")
    private String providerName;

    @Schema(description = "基础请求 URL")
    private String baseUrl;

    @Schema(description = "服务商 Logo URL")
    private String providerLogo;

    @Schema(description = "服务商官网 URL")
    private String providerWebsite;

    @Schema(description = "备注")
    private String remark;
}
