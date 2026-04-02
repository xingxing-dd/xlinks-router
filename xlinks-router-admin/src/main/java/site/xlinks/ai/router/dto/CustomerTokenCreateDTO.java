package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Customer token create request.
 */
@Data
@Schema(description = "Customer token create request")
public class CustomerTokenCreateDTO {

    @NotBlank(message = "Customer identifier must not be blank")
    @Schema(description = "Customer account identifier: username, phone, or email", required = true)
    private String customerName;

    @NotBlank(message = "Token name must not be blank")
    @Schema(description = "Token name", required = true)
    private String tokenName;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Expire time")
    private LocalDateTime expireTime;

    @Schema(description = "Allowed model codes in JSON array string")
    private String allowedModels;

    @Schema(description = "Remark")
    private String remark;
}
