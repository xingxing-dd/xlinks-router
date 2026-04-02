package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider token create request.
 */
@Data
@Schema(description = "Provider token create request")
public class ProviderTokenCreateDTO {

    @NotNull(message = "Provider ID must not be null")
    @Schema(description = "Provider ID", required = true)
    private Long providerId;

    @NotBlank(message = "Token name must not be blank")
    @Schema(description = "Token name", required = true)
    private String tokenName;

    @NotBlank(message = "Token value must not be blank")
    @Schema(description = "Token value", required = true)
    private String tokenValue;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer tokenStatus = 1;

    @Schema(description = "Quota total")
    private Long quotaTotal;

    @Schema(description = "Expire time")
    private LocalDateTime expireTime;

    @Schema(description = "Remark")
    private String remark;
}
