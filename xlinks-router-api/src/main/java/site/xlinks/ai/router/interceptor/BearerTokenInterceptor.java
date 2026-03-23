package site.xlinks.ai.router.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.xlinks.ai.router.openai.error.OpenAIErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Extracts Bearer token from Authorization header and exposes it to controllers via request attribute.
 * If missing/invalid, responds with OpenAI-compatible error payload and 401 status.
 */
@Slf4j
@Component
public class BearerTokenInterceptor implements HandlerInterceptor {

    public static final String ATTR_BEARER_TOKEN = "xlinks.bearerToken";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            writeUnauthorized(response, "Missing Authorization header");
            return false;
        }

        String token = parseBearerToken(authorization);
        if (token == null || token.isEmpty()) {
            writeUnauthorized(response, "Invalid Authorization header, expected: Bearer {token}");
            return false;
        }

        request.setAttribute(ATTR_BEARER_TOKEN, token);
        return true;
    }

    private String parseBearerToken(String authorization) {
        // RFC6750: "Bearer" scheme is case-insensitive; tolerate extra spaces.
        String trimmed = authorization.trim();
        if (trimmed.length() < 7) {
            return null;
        }
        // Accept "Bearer " or "bearer "
        if (!trimmed.regionMatches(true, 0, "Bearer", 0, "Bearer".length())) {
            return null;
        }
        return trimmed.substring("Bearer".length()).trim();
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        // Keep response minimal and OpenAI-compatible.
        response.resetBuffer();
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");

        String body = OpenAIErrorResponse.unauthorized(message).toJson();
        response.getWriter().write(body);
        response.flushBuffer();

        log.warn("Unauthorized request rejected: {}", message);
    }
}
