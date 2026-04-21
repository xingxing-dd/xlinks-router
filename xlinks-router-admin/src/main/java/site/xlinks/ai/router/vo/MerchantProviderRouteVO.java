package site.xlinks.ai.router.vo;

import lombok.Data;

@Data
public class MerchantProviderRouteVO {

    private Long id;

    private Long modelId;

    private String modelCode;

    private String modelName;

    private Long providerId;

    private String providerCode;

    private String providerName;
}
