package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Provider token update request.
 */
@Data
@Schema(description = "Provider token update request")
public class ProviderTokenUpdateDTO {

    @Schema(description = "Token name")
    private String tokenName;

    @Schema(description = "Token value")
    private String tokenValue;

    @Schema(description = "Quota total")
    private Long quotaTotal;

    @Schema(description = "Expire time")
    private LocalDateTime expireTime;

    @Schema(description = "Remark")
    private String remark;
}
