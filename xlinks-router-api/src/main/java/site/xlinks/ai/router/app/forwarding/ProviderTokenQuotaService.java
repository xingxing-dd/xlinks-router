package site.xlinks.ai.router.app.forwarding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.infrastructure.cache.DistributedRouteCacheRepository;
import site.xlinks.ai.router.infrastructure.cache.RoutingSnapshotCacheService;
import site.xlinks.ai.router.entity.ProviderToken;
import site.xlinks.ai.router.mapper.ProviderTokenMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderTokenQuotaService {

    private final ProviderTokenMapper providerTokenMapper;
    private final DistributedRouteCacheRepository distributedRouteCacheRepository;
    private final RoutingSnapshotCacheService routingSnapshotCacheService;

    public void syncUsage(Long providerTokenId, Integer totalTokens) {
        if (providerTokenId == null || totalTokens == null || totalTokens <= 0) {
            return;
        }
        ProviderToken providerToken = providerTokenMapper.selectById(providerTokenId);
        if (providerToken == null) {
            return;
        }
        long currentUsed = providerToken.getQuotaUsed() == null ? 0L : providerToken.getQuotaUsed();
        providerToken.setQuotaUsed(currentUsed + totalTokens);
        providerToken.setLastUsedAt(LocalDateTime.now());
        providerTokenMapper.updateById(providerToken);
        distributedRouteCacheRepository.putProviderTokenById(providerToken);

        Long providerId = providerToken.getProviderId();
        if (providerId == null) {
            return;
        }
        List<ProviderToken> cachedTokens = distributedRouteCacheRepository.getProviderTokensByProviderId(providerId);
        if (cachedTokens.isEmpty()) {
            routingSnapshotCacheService.refreshProviderTokensByProviderId(providerId);
            return;
        }
        List<ProviderToken> updatedTokens = new ArrayList<>(cachedTokens.size());
        for (ProviderToken token : cachedTokens) {
            if (token != null && providerTokenId.equals(token.getId())) {
                updatedTokens.add(providerToken);
            } else {
                updatedTokens.add(token);
            }
        }
        distributedRouteCacheRepository.putProviderTokensByProviderId(providerId, updatedTokens);
        routingSnapshotCacheService.refreshProviderTokensByProviderId(providerId);
    }
}
