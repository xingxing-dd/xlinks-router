package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Model endpoint update request.
 */
@Data
@Schema(description = "Model endpoint update request")
public class ModelEndpointUpdateDTO {

    @Schema(description = "Endpoint code")
    private String endpointCode;

    @Schema(description = "Endpoint name")
    private String endpointName;

    @Schema(description = "Endpoint URL")
    private String endpointUrl;

    @Schema(description = "Remark")
    private String remark;
}
