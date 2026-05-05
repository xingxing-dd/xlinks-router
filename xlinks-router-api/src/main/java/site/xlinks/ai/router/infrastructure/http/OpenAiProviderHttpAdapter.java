package site.xlinks.ai.router.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.app.forwarding.model.ForwardingDecision;
import site.xlinks.ai.router.infrastructure.http.model.ProviderInvokeContext;
import site.xlinks.ai.router.protocol.model.ForwardProtocol;
import site.xlinks.ai.router.protocol.model.ForwardRequest;

@Component
public class OpenAiProviderHttpAdapter extends AbstractOkHttpProviderHttpAdapter {

    public OpenAiProviderHttpAdapter(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        super(okHttpClient, objectMapper);
    }

    @Override
    public boolean supports(ForwardProtocol protocol) {
        return protocol == ForwardProtocol.COMPLETIONS
                || protocol == ForwardProtocol.CHAT_COMPLETIONS
                || protocol == ForwardProtocol.RESPONSES
                || protocol == ForwardProtocol.MODELS;
    }

    @Override
    protected Request buildRequest(ForwardingDecision decision, ProviderInvokeContext context) {
        ForwardRequest request = decision.getRequest();
        String url = buildRequestUrl(context.getBaseUrl(), request.getProtocol().getProviderPath());
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + context.getProviderToken())
                .addHeader("Accept", request.isStream() ? "text/event-stream" : "application/json");
        if (request.getProtocol() == ForwardProtocol.MODELS) {
            return builder.get().build();
        }
        return builder.addHeader("Content-Type", "application/json")
                .post(buildJsonRequestBody(request, context))
                .build();
    }
}
