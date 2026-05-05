package site.xlinks.ai.router.protocol.controller.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
