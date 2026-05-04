package site.xlinks.ai.router.distributed.infrastructure.http.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProviderInvokeContext {

    private Long providerId;

    private Long providerTokenId;

    private String baseUrl;

    private String providerToken;

    private String providerModelCode;

    private Integer requestTimeoutMs;

    private Integer streamFirstResponseTimeoutMs;
}
