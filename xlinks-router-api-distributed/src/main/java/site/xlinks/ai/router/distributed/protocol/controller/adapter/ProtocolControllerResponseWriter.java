package site.xlinks.ai.router.distributed.protocol.controller.adapter;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.distributed.app.forwarding.model.ForwardingAsyncStreamBody;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogCollector.RequestChainLogSession;
import site.xlinks.ai.router.distributed.support.logging.RequestChainLogType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 将统一转发结果写回协议控制器响应。
 * 非流式场景直接同步输出，流式场景使用 Servlet 非阻塞写出。
 */
@Component
public class ProtocolControllerResponseWriter {

    private final TaskExecutor forwardingStreamTaskExecutor;

    @Value("${xlinks.router.sse.timeout-ms:900000}")
    private long asyncTimeoutMs;

    @Value("${xlinks.router.sse.bridge.queue-capacity:1024}")
    private int queueCapacity;

    @Value("${xlinks.router.sse.bridge.chunk-size:20480}")
    private int chunkSize;

    @Value("${xlinks.router.sse.bridge.queue-offer-timeout-ms:1000}")
    private long queueOfferTimeoutMs;

    public ProtocolControllerResponseWriter(
            @Qualifier("forwardingStreamTaskExecutor") TaskExecutor forwardingStreamTaskExecutor) {
        this.forwardingStreamTaskExecutor = forwardingStreamTaskExecutor;
    }

    public void write(Object forwardingResult,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {
        if (!(forwardingResult instanceof ResponseEntity<?> responseEntity)) {
            throw new IllegalStateException("Unsupported forwarding result: " + forwardingResult);
        }

        applyResponseMetadata(responseEntity, response);
        Object body = responseEntity.getBody();
        if (body instanceof ForwardingAsyncStreamBody asyncStreamBody) {
            writeAsyncStream(asyncStreamBody, request, response);
        } else {
            writeDirectBody(body, response);
        }
    }

    private void applyResponseMetadata(ResponseEntity<?> responseEntity, HttpServletResponse response) {
        response.setStatus(responseEntity.getStatusCode().value());
        for (Map.Entry<String, java.util.List<String>> entry : responseEntity.getHeaders().entrySet()) {
            for (String headerValue : entry.getValue()) {
                response.addHeader(entry.getKey(), headerValue);
            }
        }
    }

    private void writeDirectBody(Object body, HttpServletResponse response) throws IOException {
        if (body == null) {
            response.flushBuffer();
            return;
        }
        ServletOutputStream outputStream = response.getOutputStream();
        if (body instanceof byte[] bytes) {
            outputStream.write(bytes);
        } else {
            if (response.getHeader(HttpHeaders.CONTENT_TYPE) == null) {
                response.setContentType("application/json;charset=UTF-8");
            }
            outputStream.write(String.valueOf(body).getBytes(StandardCharsets.UTF_8));
        }
        outputStream.flush();
    }

    private void writeAsyncStream(ForwardingAsyncStreamBody asyncStreamBody,
                                  HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        AsyncContext asyncContext = request.startAsync();
        asyncContext.setTimeout(asyncTimeoutMs);
        AsyncStreamBridge bridge = new AsyncStreamBridge(
                asyncContext,
                response.getOutputStream(),
                asyncStreamBody,
                RequestChainLogCollector.captureCurrent(),
                Math.max(queueCapacity, 8),
                Math.max(chunkSize, 1024),
                Math.max(queueOfferTimeoutMs, 100L)
        );
        bridge.start(forwardingStreamTaskExecutor);
    }

    private static final class AsyncStreamBridge implements WriteListener, AsyncListener {

        private static final StreamChunk END_OF_STREAM = new StreamChunk(new byte[0], true);

        private final AsyncContext asyncContext;
        private final ServletOutputStream outputStream;
        private final ForwardingAsyncStreamBody asyncStreamBody;
        private final RequestChainLogSession logSession;
        private final BlockingQueue<StreamChunk> chunks;
        private final int chunkSize;
        private final long queueOfferTimeoutMs;
        private final AtomicBoolean terminated = new AtomicBoolean(false);
        private final AtomicBoolean draining = new AtomicBoolean(false);

        private AsyncStreamBridge(AsyncContext asyncContext,
                                  ServletOutputStream outputStream,
                                  ForwardingAsyncStreamBody asyncStreamBody,
                                  RequestChainLogSession logSession,
                                  int queueCapacity,
                                  int chunkSize,
                                  long queueOfferTimeoutMs) {
            this.asyncContext = asyncContext;
            this.outputStream = outputStream;
            this.asyncStreamBody = asyncStreamBody;
            this.logSession = logSession;
            this.chunks = new ArrayBlockingQueue<>(queueCapacity);
            this.chunkSize = chunkSize;
            this.queueOfferTimeoutMs = queueOfferTimeoutMs;
        }

        private void start(TaskExecutor taskExecutor) throws IOException {
            asyncContext.addListener(this);
            outputStream.setWriteListener(this);
            taskExecutor.execute(this::readLoop);
        }

        /**
         * 读取线程只负责从上游拉取数据并入队，不直接执行 Servlet 写出。
         */
        private void readLoop() {
            try {
                byte[] buffer = new byte[chunkSize];
                while (!terminated.get()) {
                    int read = withLogSession(() -> asyncStreamBody.read(buffer));
                    if (read < 0) {
                        enqueue(END_OF_STREAM);
                        triggerDrainIfReady();
                        return;
                    }
                    if (read == 0) {
                        continue;
                    }
                    enqueue(new StreamChunk(Arrays.copyOf(buffer, read), false));
                    triggerDrainIfReady();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                fail(ex);
            } catch (Exception ex) {
                fail(ex);
            }
        }

        private void enqueue(StreamChunk chunk) throws InterruptedException {
            while (!terminated.get()) {
                if (chunks.offer(chunk, queueOfferTimeoutMs, TimeUnit.MILLISECONDS)) {
                    return;
                }
            }
        }

        /**
         * 仅在容器明确可写时尝试补一次写出，避免积压数据等待下一次回调。
         */
        private void triggerDrainIfReady() {
            if (terminated.get() || !outputStream.isReady()) {
                return;
            }
            withLogSessionRunnable(this::drainQueue);
        }

        @Override
        public void onWritePossible() {
            withLogSessionRunnable(this::drainQueue);
        }

        private void drainQueue() {
            if (!draining.compareAndSet(false, true)) {
                return;
            }
            try {
                while (!terminated.get()) {
                    if (!outputStream.isReady()) {
                        return;
                    }
                    StreamChunk chunk = chunks.peek();
                    if (chunk == null) {
                        return;
                    }
                    if (chunk.endOfStream()) {
                        chunks.poll();
                        asyncStreamBody.completeSuccess();
                        completeAsync();
                        return;
                    }
                    outputStream.write(chunk.bytes());
                    chunks.poll();
                }
            } catch (Exception ex) {
                fail(ex);
            } finally {
                draining.set(false);
                if (!terminated.get() && !chunks.isEmpty() && outputStream.isReady()) {
                    triggerDrainIfReady();
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            fail(throwable);
        }

        @Override
        public void onComplete(AsyncEvent event) {
            terminated.set(true);
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            fail(event.getThrowable() == null
                    ? new IOException("异步流式响应超时")
                    : event.getThrowable());
        }

        @Override
        public void onError(AsyncEvent event) {
            fail(event.getThrowable());
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            event.getAsyncContext().addListener(this);
        }

        private void fail(Throwable throwable) {
            if (!terminated.compareAndSet(false, true)) {
                return;
            }
            String message = throwable == null || throwable.getMessage() == null || throwable.getMessage().isBlank()
                    ? "流式响应写出失败"
                    : throwable.getMessage();
            withLogSessionRunnable(() -> {
                RequestChainLogCollector.record(RequestChainLogType.SSE_TRANSFER_FAILED, "-", message);
                asyncStreamBody.abort(message);
            });
            completeAsync();
        }

        private void completeAsync() {
            try {
                asyncContext.complete();
            } catch (IllegalStateException ignore) {
                // 请求已结束时忽略重复 complete。
            }
        }

        private <T> T withLogSession(LogAction<T> action) throws Exception {
            RequestChainLogCollector.bind(logSession);
            try {
                return action.execute();
            } finally {
                RequestChainLogCollector.clear();
            }
        }

        private void withLogSessionRunnable(LogRunnable action) {
            RequestChainLogCollector.bind(logSession);
            try {
                action.run();
            } finally {
                RequestChainLogCollector.clear();
            }
        }
    }

    @FunctionalInterface
    private interface LogAction<T> {
        T execute() throws Exception;
    }

    @FunctionalInterface
    private interface LogRunnable {
        void run();
    }

    private record StreamChunk(byte[] bytes, boolean endOfStream) {
    }
}
