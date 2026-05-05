package site.xlinks.ai.router.distributed.app.forwarding.model;

public record ProviderPermitLease(Long providerId,
                                  Long providerTokenId,
                                  String providerTokenName,
                                  String permitId,
                                  ProviderRuntimePolicy runtimePolicy) {
}
