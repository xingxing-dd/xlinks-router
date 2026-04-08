package site.xlinks.ai.router.client.dto.model;

import lombok.Data;

import java.util.List;

@Data
public class ModelDetailResponse {
    private Long id;
    private String name;
    private String provider;
    private String description;
    private String inputPrice;
    private String outputPrice;
    private String cacheHitPrice;
    private String contextWindow;
    private List<ModelRouteItemResponse> routes;
}
