package site.xlinks.ai.router.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.interceptor.BearerTokenInterceptor;
import site.xlinks.ai.router.openai.error.OpenAIExceptionHandler;
import site.xlinks.ai.router.service.ProtocolProxyService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenAIProxyControllerTest {

    private ProtocolProxyService proxyService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        proxyService = mock(ProtocolProxyService.class);
        OpenAIProxyController controller = new OpenAIProxyController(
                proxyService,
                new ObjectMapper(),
                new SyncTaskExecutor()
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new OpenAIExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnOpenAICompatibleModelsPayloadWithoutResultWrapper() throws Exception {
        when(proxyService.listModels("customer-token")).thenReturn(Map.of(
                "object", "list",
                "data", List.of(Map.of(
                        "id", "gpt-4",
                        "object", "model",
                        "created", 0,
                        "owned_by", "openai"
                ))
        ));

        mockMvc.perform(get("/v1/models")
                        .requestAttr(BearerTokenInterceptor.ATTR_BEARER_TOKEN, "customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data[0].id").value("gpt-4"))
                .andExpect(jsonPath("$.data[0].object").value("model"))
                .andExpect(jsonPath("$.data[0].created").value(0))
                .andExpect(jsonPath("$.data[0].owned_by").value("openai"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void shouldRenderOpenAICompatibleErrorForModelsListFailures() throws Exception {
        when(proxyService.listModels("bad-token"))
                .thenThrow(new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid API key"));

        mockMvc.perform(get("/v1/models")
                        .requestAttr(BearerTokenInterceptor.ATTR_BEARER_TOKEN, "bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.message").value("Invalid API key"))
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"))
                .andExpect(jsonPath("$.error.code").value("invalid_api_key"))
                .andExpect(jsonPath("$.code").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
