package site.xlinks.ai.router.client.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelRouteItemResponse {
    private Long providerId;
    private String providerName;
    private String modelName;
    private Integer priority;
}
