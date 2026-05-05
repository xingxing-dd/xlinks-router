package site.xlinks.ai.router.support.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.support.logging.RequestChainLogCollector.RequestChainLogSession;
import site.xlinks.ai.router.support.logging.RequestChainLogType;

import java.io.IOException;
import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = StringUtils.trimToEmpty(request.getHeader(TRACE_ID_HEADER));
        if (traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        RequestChainLogSession logSession = RequestChainLogCollector.start(request, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (request.isAsyncStarted()) {
                request.getAsyncContext().addListener(new RequestChainAsyncListener(logSession, response));
            } else {
                RequestChainLogCollector.markResponseStatus(response.getStatus());
                RequestChainLogCollector.flush(logSession);
            }
            RequestChainLogCollector.clear();
            MDC.remove(TRACE_ID);
        }
    }

    private static final class RequestChainAsyncListener implements AsyncListener {

        private final RequestChainLogSession logSession;
        private final HttpServletResponse response;

        private RequestChainAsyncListener(RequestChainLogSession logSession, HttpServletResponse response) {
            this.logSession = logSession;
            this.response = response;
        }

        @Override
        public void onComplete(AsyncEvent event) {
            RequestChainLogCollector.bind(logSession);
            try {
                RequestChainLogCollector.markResponseStatus(response.getStatus());
                RequestChainLogCollector.flush(logSession);
            } finally {
                RequestChainLogCollector.clear();
            }
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            RequestChainLogCollector.bind(logSession);
            try {
                RequestChainLogCollector.record(RequestChainLogType.ASYNC_TIMEOUT);
                RequestChainLogCollector.markUnexpectedFailure("请求异步处理超时", event.getThrowable());
            } finally {
                RequestChainLogCollector.clear();
            }
        }

        @Override
        public void onError(AsyncEvent event) {
            RequestChainLogCollector.bind(logSession);
            try {
                RequestChainLogCollector.record(RequestChainLogType.ASYNC_ERROR);
                RequestChainLogCollector.markUnexpectedFailure("请求异步处理异常", event.getThrowable());
            } finally {
                RequestChainLogCollector.clear();
            }
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }
    }
}
