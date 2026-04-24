package site.xlinks.ai.router.service;

/**
 * Acquired provider-token permit with resolved runtime policy.
 */
public record ProviderPermitLease(Long providerId,
                                  Long providerTokenId,
                                  String providerTokenName,
                                  String providerTokenValue,
                                  String permitId,
                                  ProxyRuntimePolicy runtimePolicy) {

    public boolean hasPermit() {
        return permitId != null && !permitId.isBlank();
    }
}
