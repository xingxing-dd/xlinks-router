package site.xlinks.ai.router.distributed.infrastructure.http.model;

import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 上游流式响应句柄，持有底层 HTTP 连接与输入流。
 * 调用方可以按需读取，也可以在客户端断开时主动中止。
 */
public final class ProviderStreamHandle implements AutoCloseable {

    private final Response response;
    private final ResponseBody responseBody;
    private final InputStream inputStream;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ProviderStreamHandle(Response response, ResponseBody responseBody, InputStream inputStream) {
        this.response = response;
        this.responseBody = responseBody;
        this.inputStream = inputStream;
    }

    public int read(byte[] buffer) throws IOException {
        return inputStream.read(buffer);
    }

    @Override
    public void close() throws IOException {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        IOException failure = null;
        try {
            inputStream.close();
        } catch (IOException ex) {
            failure = ex;
        }
        try {
            responseBody.close();
        } catch (Exception ex) {
            if (failure == null && ex instanceof IOException ioException) {
                failure = ioException;
            }
        }
        try {
            response.close();
        } catch (Exception ex) {
            if (failure == null && ex instanceof IOException ioException) {
                failure = ioException;
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public void closeQuietly() {
        try {
            close();
        } catch (Exception ignore) {
            // 主动中止流式会话时忽略关闭异常。
        }
    }
}
