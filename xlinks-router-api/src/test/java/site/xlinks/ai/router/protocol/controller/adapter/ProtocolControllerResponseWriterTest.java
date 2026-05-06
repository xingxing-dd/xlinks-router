package site.xlinks.ai.router.protocol.controller.adapter;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import site.xlinks.ai.router.app.forwarding.model.ForwardingAsyncStreamBody;
import site.xlinks.ai.router.infrastructure.http.model.ProviderStreamHandle;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolControllerResponseWriterTest {

    private final ProtocolControllerResponseWriter writer = new ProtocolControllerResponseWriter(task -> {
    });

    @Test
    void shouldWriteDirectStringBodyToServletResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(
                ResponseEntity.status(429)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8")
                        .body("{\"error\":\"rate limit\"}"),
                request,
                response
        );

        assertEquals(429, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getHeader(HttpHeaders.CONTENT_TYPE));
        assertEquals("{\"error\":\"rate limit\"}", response.getContentAsString());
    }

    @Test
    void shouldAbortAndCleanupWhenAsyncStreamBridgeStartupFails() {
        ProtocolControllerResponseWriter writer = new ProtocolControllerResponseWriter(task -> {
        });
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean cleanupCalled = new AtomicBoolean(false);
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        ForwardingAsyncStreamBody asyncStreamBody = new ForwardingAsyncStreamBody(
                providerStreamHandle(new ByteArrayInputStream("stream".getBytes(StandardCharsets.UTF_8))),
                1024,
                payload -> {
                },
                message -> errorCalled.set(true),
                () -> cleanupCalled.set(true)
        );

        assertThrows(UnsupportedOperationException.class, () -> writer.write(
                ResponseEntity.ok(asyncStreamBody),
                request,
                response
        ));

        assertTrue(errorCalled.get());
        assertTrue(cleanupCalled.get());
    }

    private ProviderStreamHandle providerStreamHandle(InputStream inputStream) {
        Request request = new Request.Builder().url("https://example.com/v1/responses").build();
        ResponseBody responseBody = ResponseBody.create("", MediaType.get("text/event-stream"));
        Response response = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(responseBody)
                .build();
        return new ProviderStreamHandle(response, responseBody, inputStream);
    }
}
