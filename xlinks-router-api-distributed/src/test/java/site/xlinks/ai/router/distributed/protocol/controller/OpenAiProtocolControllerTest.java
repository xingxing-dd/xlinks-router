package site.xlinks.ai.router.distributed.protocol.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import site.xlinks.ai.router.distributed.app.forwarding.ForwardingApplicationService;
import site.xlinks.ai.router.distributed.protocol.controller.adapter.ProtocolControllerResponseWriter;
import site.xlinks.ai.router.distributed.protocol.handler.ProtocolExceptionHandler;
import site.xlinks.ai.router.distributed.protocol.interceptor.ProtocolCustomerTokenInterceptor;
import site.xlinks.ai.router.distributed.protocol.service.CustomerTokenResolver;
import site.xlinks.ai.router.distributed.protocol.service.OpenAiModelsService;
import site.xlinks.ai.router.distributed.protocol.service.ProtocolRequestParser;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenAiProtocolControllerTest {

    private OpenAiModelsService openAiModelsService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        openAiModelsService = mock(OpenAiModelsService.class);
        OpenAiProtocolController controller = new OpenAiProtocolController(
                mock(ForwardingApplicationService.class),
                mock(ProtocolRequestParser.class),
                mock(ProtocolControllerResponseWriter.class),
                openAiModelsService
        );
        ProtocolCustomerTokenInterceptor interceptor = new ProtocolCustomerTokenInterceptor(new CustomerTokenResolver());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(interceptor)
                .setControllerAdvice(new ProtocolExceptionHandler())
                .build();
    }

    @Test
    void modelsShouldReturnOpenAiListResponse() throws Exception {
        when(openAiModelsService.listModels(any())).thenReturn(Map.of(
                "object", "list",
                "data", List.of(
                        Map.of(
                                "id", "gpt-4o-mini",
                                "object", "model",
                                "created", 1767323045L,
                                "owned_by", "xlinks-router"
                        )
                )
        ));

        mockMvc.perform(get("/v1/models")
                        .header("Authorization", "Bearer customer-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data[0].id").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.data[0].object").value("model"))
                .andExpect(jsonPath("$.data[0].owned_by").value("xlinks-router"));

        verify(openAiModelsService).listModels(any());
    }

    @Test
    void modelsShouldReturnUnauthorizedWhenAuthorizationHeaderMissing() throws Exception {
        mockMvc.perform(get("/v1/models").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"))
                .andExpect(jsonPath("$.error.code").value("invalid_api_key"));

        verifyNoInteractions(openAiModelsService);
    }
}
