package site.xlinks.ai.router.distributed.infrastructure.cache;

import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;

public final class RedisCacheKeys {

    private static final String PREFIX = "api:cache:";

    private RedisCacheKeys() {
    }

    public static String modelByCode(String modelCode) {
        return PREFIX + "model:code:" + modelCode;
    }

    public static String modelList() {
        return PREFIX + "models:list";
    }

    public static String providerById(Long providerId) {
        return PREFIX + "provider:id:" + providerId;
    }

    public static String providerProtocolMatcher(Long providerId) {
        return PREFIX + "provider:protocols:" + providerId;
    }

    public static String providerModelsByModelId(Long modelId) {
        return PREFIX + "provider-models:model-id:" + modelId;
    }

    public static String providerTokensByProviderId(Long providerId) {
        return PREFIX + "provider-tokens:provider-id:" + providerId;
    }

    public static String providerTokenById(Long providerTokenId) {
        return PREFIX + "provider-token:id:" + providerTokenId;
    }

    public static String routingIndex(Long modelId, ForwardProtocol protocol) {
        return PREFIX + "routing:model-id:" + modelId + ":protocol:" + protocol.getCode();
    }

    public static String customerTokenByValue(String tokenValue) {
        return PREFIX + "customer-token:value:" + tokenValue;
    }

    public static String customerTokenById(Long tokenId) {
        return PREFIX + "customer-token:id:" + tokenId;
    }

    public static String customerAllowedModels(Long tokenId) {
        return PREFIX + "customer-token:allowed-models:" + tokenId;
    }

    public static String planAllowedModels(Long planId) {
        return PREFIX + "plan:allowed-models:" + planId;
    }

    public static String merchantPreferredProvider(Long accountId, Long modelId) {
        return PREFIX + "merchant-route:account-id:" + accountId + ":model-id:" + modelId;
    }

    public static String providerFailure(Long providerId) {
        return PREFIX + "provider-failure:" + providerId;
    }

    public static String providerTokenFailure(Long providerTokenId) {
        return PREFIX + "provider-token-failure:" + providerTokenId;
    }

    public static String cacheVersion(String scope) {
        return PREFIX + "version:" + scope;
    }
}
