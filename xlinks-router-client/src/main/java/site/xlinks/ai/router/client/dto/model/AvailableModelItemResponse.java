package site.xlinks.ai.router.client.dto.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableModelItemResponse {
    private Long id;
    private String name;
    private String provider;
    private String description;
    private String inputPrice;
    private String outputPrice;
    private String cacheHitPrice;
    private String contextWindow;
    private String status;
}
