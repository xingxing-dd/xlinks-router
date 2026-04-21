package site.xlinks.ai.router.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MerchantProviderRouteConfigDTO {

    @NotNull(message = "Model cannot be null")
    private Long modelId;

    @NotNull(message = "Provider cannot be null")
    private Long providerId;
}
