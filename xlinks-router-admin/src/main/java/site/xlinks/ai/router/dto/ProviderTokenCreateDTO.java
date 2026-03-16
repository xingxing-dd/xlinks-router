package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider Token 创建 DTO
 */
@Data
@Schema(description = "Provider Token 创建请求")
public class ProviderTokenCreateDTO {

    @Schema(description = "Provider ID", required = true)
    @NotNull(message = "Provider ID 不能为空")
    private Long providerId;

    @Schema(description = "Token 名称", required = true)
    @NotBlank(message = "Token 名称不能为空")
    private String tokenName;

    @Schema(description = "Token 值", required = true)
    @NotBlank(message = "Token 值不能为空")
    private String tokenValue;

    @Schema(description = "Token 状态：1-正常，0-禁用")
    private Integer tokenStatus = 1;

    @Schema(description = "配额总量")
    private Long quotaTotal;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
