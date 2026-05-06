package site.xlinks.ai.router.infrastructure.cache;

import site.xlinks.ai.router.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.infrastructure.cache.model.ProviderProtocolRule;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Duration;
import java.util.List;

public interface DistributedRouteCacheRepository {

    Model getModelByCode(String modelCode);

    void putModelByCode(Model model);

    List<Model> getModelList();

    void putModelList(List<Model> models);

    Provider getProviderById(Long providerId);

    void putProviderById(Provider provider);

    ProviderProtocolRule getProviderProtocolRule(Long providerId);

    void putProviderProtocolRule(Long providerId, ProviderProtocolRule rule);

    List<ProviderModel> getProviderModelsByModelId(Long modelId);

    void putProviderModelsByModelId(Long modelId, List<ProviderModel> providerModels);

    List<ProviderModel> getRoutingIndex(Long modelId, ForwardProtocol protocol);

    void putRoutingIndex(Long modelId, ForwardProtocol protocol, List<ProviderModel> providerModels);

    List<ProviderToken> getProviderTokensByProviderId(Long providerId);

    void putProviderTokensByProviderId(Long providerId, List<ProviderToken> providerTokens);

    ProviderToken getProviderTokenById(Long providerTokenId);

    void putProviderTokenById(ProviderToken providerToken);

    CustomerToken getCustomerTokenByValue(String tokenValue);

    void putCustomerTokenByValue(CustomerToken customerToken);

    CustomerToken getCustomerTokenById(Long tokenId);

    void putCustomerTokenById(CustomerToken customerToken);

    AllowedModelsRule getCustomerAllowedModelsRule(Long tokenId);

    void putCustomerAllowedModelsRule(Long tokenId, AllowedModelsRule rule);

    AllowedModelsRule getPlanAllowedModelsRule(Long planId);

    void putPlanAllowedModelsRule(Long planId, AllowedModelsRule rule);

    Long getMerchantPreferredProvider(Long accountId, Long modelId);

    void putMerchantPreferredProvider(Long accountId, Long modelId, Long providerId);

    ProviderFailureState getProviderFailureState(Long providerId);

    void putProviderFailureState(Long providerId, ProviderFailureState state, Duration ttl);

    void deleteProviderFailureState(Long providerId);

    ProviderFailureState getProviderTokenFailureState(Long providerTokenId);

    void putProviderTokenFailureState(Long providerTokenId, ProviderFailureState state, Duration ttl);

    void deleteProviderTokenFailureState(Long providerTokenId);

    long nextProviderTokenCursor(Long providerId);
}
