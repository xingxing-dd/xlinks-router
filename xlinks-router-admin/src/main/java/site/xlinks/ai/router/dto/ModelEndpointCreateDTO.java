package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Model endpoint create request.
 */
@Data
@Schema(description = "Model endpoint create request")
public class ModelEndpointCreateDTO {

    @NotBlank(message = "Endpoint code must not be blank")
    @Schema(description = "Endpoint code", required = true)
    private String endpointCode;

    @NotBlank(message = "Endpoint name must not be blank")
    @Schema(description = "Endpoint name", required = true)
    private String endpointName;

    @NotBlank(message = "Endpoint URL must not be blank")
    @Schema(description = "Endpoint URL", required = true)
    private String endpointUrl;

    @Schema(description = "Status: 1-enabled, 0-disabled")
    private Integer status = 1;

    @Schema(description = "Remark")
    private String remark;
}
