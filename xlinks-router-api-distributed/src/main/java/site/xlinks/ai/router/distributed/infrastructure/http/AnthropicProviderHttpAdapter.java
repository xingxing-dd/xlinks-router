package site.xlinks.ai.router.distributed.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingPreparation;
import site.xlinks.ai.router.distributed.infrastructure.http.model.ProviderInvokeContext;
import site.xlinks.ai.router.distributed.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.distributed.protocol.model.ForwardRequest;

@Component
public class AnthropicProviderHttpAdapter extends AbstractOkHttpProviderHttpAdapter {

    private static final String DEFAULT_ANTHROPIC_VERSION = "2023-06-01";

    public AnthropicProviderHttpAdapter(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        super(okHttpClient, objectMapper);
    }

    @Override
    public boolean supports(ForwardProtocol protocol) {
        return protocol == ForwardProtocol.ANTHROPIC_MESSAGES;
    }

    @Override
    protected Request buildRequest(ForwardingPreparation preparation, ProviderInvokeContext context) {
        ForwardRequest request = preparation.getRequest();
        String url = context.getBaseUrl() + request.getProtocol().getProviderPath();
        String anthropicVersion = request.getPassthroughHeaders() == null
                ? null
                : request.getPassthroughHeaders().get("anthropic-version");
        if (anthropicVersion == null || anthropicVersion.isBlank()) {
            anthropicVersion = DEFAULT_ANTHROPIC_VERSION;
        }

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", request.isStream() ? "text/event-stream" : "application/json")
                .addHeader("x-api-key", context.getProviderToken())
                .addHeader("Authorization", "Bearer " + context.getProviderToken())
                .addHeader("anthropic-version", anthropicVersion);
        if (request.getPassthroughHeaders() != null) {
            String anthropicBeta = request.getPassthroughHeaders().get("anthropic-beta");
            if (anthropicBeta != null && !anthropicBeta.isBlank()) {
                builder.addHeader("anthropic-beta", anthropicBeta);
            }
        }
        return builder.post(buildJsonRequestBody(request, context)).build();
    }
}
