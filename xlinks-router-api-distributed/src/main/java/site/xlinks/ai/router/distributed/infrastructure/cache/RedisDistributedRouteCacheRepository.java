package site.xlinks.ai.router.distributed.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.AllowedModelsRule;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderFailureState;
import site.xlinks.ai.router.distributed.infrastructure.cache.model.ProviderProtocolRule;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Provider;
import site.xlinks.ai.router.entity.ProviderModel;
import site.xlinks.ai.router.entity.ProviderToken;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisDistributedRouteCacheRepository implements DistributedRouteCacheRepository {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Model getModelByCode(String modelCode) {
        return readObject(RedisCacheKeys.modelByCode(modelCode), Model.class);
    }

    @Override
    public void putModelByCode(Model model) {
        if (model == null || model.getModelCode() == null || model.getModelCode().isBlank()) {
            return;
        }
        writeObject(RedisCacheKeys.modelByCode(model.getModelCode()), model, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public List<Model> getModelList() {
        return readList(RedisCacheKeys.modelList(), new TypeReference<List<Model>>() {
        });
    }

    @Override
    public void putModelList(List<Model> models) {
        writeObject(RedisCacheKeys.modelList(), models, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public Provider getProviderById(Long providerId) {
        return readObject(RedisCacheKeys.providerById(providerId), Provider.class);
    }

    @Override
    public void putProviderById(Provider provider) {
        if (provider == null || provider.getId() == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerById(provider.getId()), provider, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public ProviderProtocolRule getProviderProtocolRule(Long providerId) {
        return readObject(RedisCacheKeys.providerProtocolMatcher(providerId), ProviderProtocolRule.class);
    }

    @Override
    public void putProviderProtocolRule(Long providerId, ProviderProtocolRule rule) {
        if (providerId == null || rule == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerProtocolMatcher(providerId), rule, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public List<ProviderModel> getProviderModelsByModelId(Long modelId) {
        return readList(RedisCacheKeys.providerModelsByModelId(modelId), new TypeReference<List<ProviderModel>>() {
        });
    }

    @Override
    public void putProviderModelsByModelId(Long modelId, List<ProviderModel> providerModels) {
        if (modelId == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerModelsByModelId(modelId), providerModels, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public List<ProviderModel> getRoutingIndex(Long modelId, ForwardProtocol protocol) {
        return readList(RedisCacheKeys.routingIndex(modelId, protocol), new TypeReference<List<ProviderModel>>() {
        });
    }

    @Override
    public void putRoutingIndex(Long modelId, ForwardProtocol protocol, List<ProviderModel> providerModels) {
        if (modelId == null || protocol == null) {
            return;
        }
        writeObject(RedisCacheKeys.routingIndex(modelId, protocol), providerModels, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public List<ProviderToken> getProviderTokensByProviderId(Long providerId) {
        return readList(RedisCacheKeys.providerTokensByProviderId(providerId), new TypeReference<List<ProviderToken>>() {
        });
    }

    @Override
    public void putProviderTokensByProviderId(Long providerId, List<ProviderToken> providerTokens) {
        if (providerId == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerTokensByProviderId(providerId), providerTokens, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public ProviderToken getProviderTokenById(Long providerTokenId) {
        return readObject(RedisCacheKeys.providerTokenById(providerTokenId), ProviderToken.class);
    }

    @Override
    public void putProviderTokenById(ProviderToken providerToken) {
        if (providerToken == null || providerToken.getId() == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerTokenById(providerToken.getId()), providerToken, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public CustomerToken getCustomerTokenByValue(String tokenValue) {
        return readObject(RedisCacheKeys.customerTokenByValue(tokenValue), CustomerToken.class);
    }

    @Override
    public void putCustomerTokenByValue(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getTokenValue() == null || customerToken.getTokenValue().isBlank()) {
            return;
        }
        writeObject(RedisCacheKeys.customerTokenByValue(customerToken.getTokenValue()), customerToken, CacheTtlPolicy.CUSTOMER_TOKEN_TTL);
    }

    @Override
    public CustomerToken getCustomerTokenById(Long tokenId) {
        return readObject(RedisCacheKeys.customerTokenById(tokenId), CustomerToken.class);
    }

    @Override
    public void putCustomerTokenById(CustomerToken customerToken) {
        if (customerToken == null || customerToken.getId() == null) {
            return;
        }
        writeObject(RedisCacheKeys.customerTokenById(customerToken.getId()), customerToken, CacheTtlPolicy.CUSTOMER_TOKEN_TTL);
    }

    @Override
    public AllowedModelsRule getCustomerAllowedModelsRule(Long tokenId) {
        return readObject(RedisCacheKeys.customerAllowedModels(tokenId), AllowedModelsRule.class);
    }

    @Override
    public void putCustomerAllowedModelsRule(Long tokenId, AllowedModelsRule rule) {
        if (tokenId == null || rule == null) {
            return;
        }
        writeObject(RedisCacheKeys.customerAllowedModels(tokenId), rule, CacheTtlPolicy.CUSTOMER_TOKEN_TTL);
    }

    @Override
    public AllowedModelsRule getPlanAllowedModelsRule(Long planId) {
        return readObject(RedisCacheKeys.planAllowedModels(planId), AllowedModelsRule.class);
    }

    @Override
    public void putPlanAllowedModelsRule(Long planId, AllowedModelsRule rule) {
        if (planId == null || rule == null) {
            return;
        }
        writeObject(RedisCacheKeys.planAllowedModels(planId), rule, CacheTtlPolicy.ROUTING_READ_MODEL_TTL);
    }

    @Override
    public Long getMerchantPreferredProvider(Long accountId, Long modelId) {
        String value = stringRedisTemplate.opsForValue().get(RedisCacheKeys.merchantPreferredProvider(accountId, modelId));
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }

    @Override
    public void putMerchantPreferredProvider(Long accountId, Long modelId, Long providerId) {
        if (accountId == null || modelId == null || providerId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                RedisCacheKeys.merchantPreferredProvider(accountId, modelId),
                String.valueOf(providerId),
                CacheTtlPolicy.ROUTING_READ_MODEL_TTL
        );
    }

    @Override
    public ProviderFailureState getProviderFailureState(Long providerId) {
        return readObject(RedisCacheKeys.providerFailure(providerId), ProviderFailureState.class);
    }

    @Override
    public void putProviderFailureState(Long providerId, ProviderFailureState state) {
        if (providerId == null || state == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerFailure(providerId), state, CacheTtlPolicy.PROVIDER_RUNTIME_STATE_TTL);
    }

    @Override
    public void deleteProviderFailureState(Long providerId) {
        if (providerId != null) {
            stringRedisTemplate.delete(RedisCacheKeys.providerFailure(providerId));
        }
    }

    @Override
    public ProviderFailureState getProviderTokenFailureState(Long providerTokenId) {
        return readObject(RedisCacheKeys.providerTokenFailure(providerTokenId), ProviderFailureState.class);
    }

    @Override
    public void putProviderTokenFailureState(Long providerTokenId, ProviderFailureState state) {
        if (providerTokenId == null || state == null) {
            return;
        }
        writeObject(RedisCacheKeys.providerTokenFailure(providerTokenId), state, CacheTtlPolicy.PROVIDER_RUNTIME_STATE_TTL);
    }

    @Override
    public void deleteProviderTokenFailureState(Long providerTokenId) {
        if (providerTokenId != null) {
            stringRedisTemplate.delete(RedisCacheKeys.providerTokenFailure(providerTokenId));
        }
    }

    private <T> T readObject(String key, Class<T> type) {
        if (key == null || type == null) {
            return null;
        }
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, type);
        } catch (Exception ex) {
            log.warn("Failed to deserialize redis cache object. key={}, type={}", key, type.getSimpleName(), ex);
            return null;
        }
    }

    private <T> List<T> readList(String key, TypeReference<List<T>> typeReference) {
        if (key == null || typeReference == null) {
            return Collections.emptyList();
        }
        String raw = stringRedisTemplate.opsForValue().get(key);
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<T> value = objectMapper.readValue(raw, typeReference);
            return value == null ? Collections.emptyList() : value;
        } catch (Exception ex) {
            log.warn("Failed to deserialize redis cache list. key={}", key, ex);
            return Collections.emptyList();
        }
    }

    private void writeObject(String key, Object value, Duration ttl) {
        if (key == null || value == null || ttl == null) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.warn("Failed to write redis cache. key={}", key, ex);
        }
    }
}
