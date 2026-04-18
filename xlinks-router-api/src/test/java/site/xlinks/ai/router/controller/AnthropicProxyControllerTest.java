package site.xlinks.ai.router.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.xlinks.ai.router.dto.StreamEvent;
import site.xlinks.ai.router.service.ProtocolProxyService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnthropicProxyControllerTest {

    private ProtocolProxyService proxyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proxyService = mock(ProtocolProxyService.class);
        AnthropicProxyController controller = new AnthropicProxyController(
                proxyService,
                new ObjectMapper(),
                new SyncTaskExecutor()
        );
        ReflectionTestUtils.setField(controller, "sseTimeoutMs", 1000L);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldRenderAnthropicSseResponseWithMetadata() throws Exception {
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<StreamEvent> consumer =
                    invocation.getArgument(2, java.util.function.Consumer.class);
            consumer.accept(StreamEvent.builder()
                    .id("evt-1")
                    .retry(1500L)
                    .event("message_start")
                    .dataLine("{\"type\":\"message_start\"}")
                    .build());
            return null;
        }).when(proxyService).forwardStream(eq("customer-token"), any(), any());

        MvcResult result = mockMvc.perform(post("/v1/messages")
                        .header("x-api-key", "customer-token")
                        .header("anthropic-version", "2023-06-01")
                        .contentType("application/json")
                        .content("{\"model\":\"claude-sonnet-4-5\",\"stream\":true,\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/event-stream;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache, no-transform"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("id:evt-1"));
        assertTrue(body.contains("retry:1500"));
        assertTrue(body.contains("event:message_start"));
        assertTrue(body.contains("data:{\"type\":\"message_start\"}"));
    }
}
