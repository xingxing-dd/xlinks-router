package site.xlinks.ai.router.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat Provider 适配器工厂
 * 根据 Provider 类型自动选择对应的适配器
 */
@Slf4j
@Component
public class ChatProviderAdapterFactory {

    private final List<ChatProviderAdapter> chatProviderAdapters;

    private final Map<String, ChatProviderAdapter> adapterMap = new ConcurrentHashMap<>();

    public ChatProviderAdapterFactory(List<ChatProviderAdapter> adapters) {
        this.chatProviderAdapters = adapters;
    }

    /**
     * 根据 Provider 类型获取对应的适配器
     *
     * @param providerType Provider 类型
     * @return 适配器实例
     */
    public ChatProviderAdapter getAdapter(String providerType) {
        ChatProviderAdapter adapter = adapterMap.get(providerType);
        if (adapter == null) {
            // 尝试获取第一个支持该类型或默认的适配器
            for (ChatProviderAdapter a : chatProviderAdapters) {
                if (a.supports(providerType)) {
                    adapterMap.put(providerType, a);
                    return a;
                }
            }
        }
        return adapter;
    }

    /**
     * 注册适配器
     *
     * @param providerType Provider 类型
     * @param adapter       适配器实例
     */
    public void registerAdapter(String providerType, ChatProviderAdapter adapter) {
        adapterMap.put(providerType, adapter);
        log.info("Registered adapter {} for provider type: {}", adapter.getClass().getSimpleName(), providerType);
    }
}
