package site.xlinks.ai.router.openai.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAIExceptionHandlerTest {

    private final OpenAIExceptionHandler handler = new OpenAIExceptionHandler();

    @Test
    void shouldReturnJsonStringForEventStreamBusinessErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.ACCEPT, "text/event-stream");

        ResponseEntity<?> responseEntity = handler.handleBusiness(
                new BusinessException(ErrorCode.UNAUTHORIZED, "invalid token"),
                request,
                response
        );

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertInstanceOf(String.class, responseEntity.getBody());
        assertTrue(String.valueOf(responseEntity.getBody()).contains("\"invalid_api_key\""));
    }

    @Test
    void shouldReturnObjectForNonStreamBusinessErrors() {
        HttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<?> responseEntity = handler.handleBusiness(
                new BusinessException(ErrorCode.PARAM_ERROR, "bad request"),
                request,
                response
        );

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertInstanceOf(OpenAIErrorResponse.class, responseEntity.getBody());
    }

    @Test
    void shouldReturnJsonStringWhenResponseAlreadyMarkedAsEventStream() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/event-stream;charset=UTF-8");

        ResponseEntity<?> responseEntity = handler.handleOther(
                new RuntimeException("boom"),
                request,
                response
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertInstanceOf(String.class, responseEntity.getBody());
        assertTrue(String.valueOf(responseEntity.getBody()).contains("\"internal_error\""));
    }

    @Test
    void shouldIgnoreClientDisconnectErrors() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setContentType("text/event-stream;charset=UTF-8");

        ResponseEntity<?> responseEntity = handler.handleOther(
                new RuntimeException("Broken pipe"),
                request,
                response
        );

        assertEquals(HttpStatus.NO_CONTENT, responseEntity.getStatusCode());
    }
}
