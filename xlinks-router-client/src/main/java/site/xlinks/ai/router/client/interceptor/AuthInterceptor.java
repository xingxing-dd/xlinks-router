package site.xlinks.ai.router.client.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.service.TokenService;

/**
 * 认证拦截器
 * 验证请求头中的Token，并设置当前用户上下文
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String TOKEN_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取Token
        String authHeader = request.getHeader(TOKEN_HEADER);

        if (authHeader != null && authHeader.startsWith(TOKEN_PREFIX)) {
            String token = authHeader.substring(TOKEN_PREFIX.length());

            // 验证Token
            var account = tokenService.validateToken(token);
            if (account != null) {
                // 设置到上下文
                CustomerAccountContext.setAccount(account);
                log.debug("Authenticated account: {}", account.getId());
                return true;
            }

        }
        // Token无效或过期
        log.debug("Invalid token for request: {}", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理上下文
        CustomerAccountContext.clear();
    }
}
