package site.xlinks.ai.router.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProtocolRequestParser {

    private final List<ProtocolRequestParsingStrategy> strategies;
    private final ProtocolRequestContextResolver protocolRequestContextResolver;
    private volatile Map<ForwardProtocol, ProtocolRequestParsingStrategy> strategyByProtocol;

    // 协议解析入口只负责分发策略，不承载具体协议细节，方便横向扩展新协议。
    public ForwardRequest parse(ForwardProtocol protocol,
                                 String requestBody,
                                 HttpServletRequest request) {
        ProtocolRequestParsingStrategy strategy = getStrategyByProtocol().get(protocol);
        if (strategy == null) {
            throw new IllegalStateException("No protocol request parsing strategy registered for " + protocol);
        }
        CustomerTokenResolver.ResolvedCustomerToken token = protocolRequestContextResolver.resolveCustomerToken(request);
        return strategy.parse(protocol, requestBody, request, token);
    }

    private Map<ForwardProtocol, ProtocolRequestParsingStrategy> buildStrategyMap(List<ProtocolRequestParsingStrategy> strategies) {
        Map<ForwardProtocol, ProtocolRequestParsingStrategy> mappings = new EnumMap<>(ForwardProtocol.class);
        for (ProtocolRequestParsingStrategy strategy : strategies) {
            for (ForwardProtocol protocol : strategy.supportedProtocols()) {
                ProtocolRequestParsingStrategy existing = mappings.putIfAbsent(protocol, strategy);
                if (existing != null) {
                    throw new IllegalStateException("Duplicate protocol request parsing strategy for " + protocol);
                }
            }
        }
        return Map.copyOf(mappings);
    }

    private Map<ForwardProtocol, ProtocolRequestParsingStrategy> getStrategyByProtocol() {
        Map<ForwardProtocol, ProtocolRequestParsingStrategy> local = strategyByProtocol;
        if (local == null) {
            local = buildStrategyMap(strategies);
            strategyByProtocol = local;
        }
        return local;
    }
}
