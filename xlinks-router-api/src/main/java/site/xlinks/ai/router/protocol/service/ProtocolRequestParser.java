package site.xlinks.ai.router.protocol.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.protocol.model.DistributedErrorCode;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProtocolRequestParser {

    private static final String ATTR_RAW_REQUEST_BODY = ProtocolRequestParser.class.getName() + ".RAW_REQUEST_BODY";

    private final List<ProtocolRequestParsingStrategy> strategies;
    private final ProtocolRequestContextResolver protocolRequestContextResolver;
    private volatile Map<ForwardProtocol, ProtocolRequestParsingStrategy> strategyByProtocol;

    // 协议解析入口只负责分发策略，不承担具体协议细节，方便横向扩展新协议。
    public ForwardRequest parse(ForwardProtocol protocol, HttpServletRequest request) {
        ProtocolRequestParsingStrategy strategy = getStrategyByProtocol().get(protocol);
        if (strategy == null) {
            throw new IllegalStateException("No protocol request parsing strategy registered for " + protocol);
        }
        CustomerTokenResolver.ResolvedCustomerToken token = protocolRequestContextResolver.resolveCustomerToken(request);
        String requestBody = readRequestBody(request);
        return strategy.parse(protocol, requestBody, request, token);
    }

    private String readRequestBody(HttpServletRequest request) {
        Object cached = request.getAttribute(ATTR_RAW_REQUEST_BODY);
        if (cached instanceof String cachedBody) {
            return cachedBody;
        }

        try {
            String requestBody = StreamUtils.copyToString(request.getInputStream(), resolveCharset(request));
            request.setAttribute(ATTR_RAW_REQUEST_BODY, requestBody);
            return requestBody;
        } catch (IOException ex) {
            throw new BusinessException(
                    DistributedErrorCode.INVALID_JSON_REQUEST.getCode(),
                    DistributedErrorCode.INVALID_JSON_REQUEST.getMessage()
            );
        }
    }

    private Charset resolveCharset(HttpServletRequest request) {
        String encoding = request == null ? null : request.getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
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
