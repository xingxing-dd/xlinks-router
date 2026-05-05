package site.xlinks.ai.router.distributed.infrastructure.cache;

import org.junit.jupiter.api.Test;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisCacheKeysTest {

    @Test
    void allCacheKeysShouldUseUnifiedRouterPrefix() {
        assertEquals("xlinks:router:model:code:gpt-4o", RedisCacheKeys.modelByCode("gpt-4o"));
        assertEquals("xlinks:router:models:list", RedisCacheKeys.modelList());
        assertEquals("xlinks:router:provider:id:1001", RedisCacheKeys.providerById(1001L));
        assertEquals("xlinks:router:provider:protocols:1001", RedisCacheKeys.providerProtocolMatcher(1001L));
        assertEquals("xlinks:router:provider-models:model-id:2001", RedisCacheKeys.providerModelsByModelId(2001L));
        assertEquals("xlinks:router:provider-tokens:provider-id:1001", RedisCacheKeys.providerTokensByProviderId(1001L));
        assertEquals("xlinks:router:provider-token:id:3001", RedisCacheKeys.providerTokenById(3001L));
        assertEquals("xlinks:router:routing:model-id:2001:protocol:chat/completions",
                RedisCacheKeys.routingIndex(2001L, ForwardProtocol.CHAT_COMPLETIONS));
        assertEquals("xlinks:router:customer-token:value:abc", RedisCacheKeys.customerTokenByValue("abc"));
        assertEquals("xlinks:router:customer-token:id:11", RedisCacheKeys.customerTokenById(11L));
        assertEquals("xlinks:router:customer-token:allowed-models:11", RedisCacheKeys.customerAllowedModels(11L));
        assertEquals("xlinks:router:plan:allowed-models:22", RedisCacheKeys.planAllowedModels(22L));
        assertEquals("xlinks:router:merchant-route:account-id:1:model-id:2",
                RedisCacheKeys.merchantPreferredProvider(1L, 2L));
        assertEquals("xlinks:router:provider-failure:1001", RedisCacheKeys.providerFailure(1001L));
        assertEquals("xlinks:router:provider-token-failure:3001", RedisCacheKeys.providerTokenFailure(3001L));
        assertEquals("xlinks:router:provider-token-cursor:1001", RedisCacheKeys.providerTokenCursor(1001L));
        assertEquals("xlinks:router:version:routing", RedisCacheKeys.cacheVersion("routing"));
    }

    @Test
    void routingAndPermitKeysShouldShareSamePrefixFamily() {
        String routingKey = RedisCacheKeys.routingIndex(2001L, ForwardProtocol.RESPONSES);
        String providerPermitKey = "xlinks:router:provider:1001:token:3001:permits";

        assertTrue(routingKey.startsWith("xlinks:router:"));
        assertTrue(providerPermitKey.startsWith("xlinks:router:"));
    }
}
