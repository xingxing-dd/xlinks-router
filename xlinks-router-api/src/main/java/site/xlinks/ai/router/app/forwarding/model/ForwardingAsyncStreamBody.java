package site.xlinks.ai.router.app.forwarding.model;

import site.xlinks.ai.router.infrastructure.http.model.ProviderStreamHandle;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 转发层暴露给控制器的异步流体。
 * 读取上游、usage 统计、permit 释放都通过这个对象串起来。
 */
public final class ForwardingAsyncStreamBody {

    private final ProviderStreamHandle streamHandle;
    private final byte[] prefetchedChunk;
    private final int prefetchedChunkLength;
    private final TailCaptureBuffer tailCaptureBuffer;
    private final Consumer<String> successCallback;
    private final Consumer<String> errorCallback;
    private final Runnable cleanupCallback;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private int prefetchedOffset;

    public ForwardingAsyncStreamBody(ProviderStreamHandle streamHandle,
                                     byte[] prefetchedChunk,
                                     int prefetchedChunkLength,
                                     int tailCaptureBytes,
                                     Consumer<String> successCallback,
                                     Consumer<String> errorCallback,
                                     Runnable cleanupCallback) {
        this.streamHandle = streamHandle;
        this.prefetchedChunk = prefetchedChunk == null ? new byte[0] : prefetchedChunk;
        this.prefetchedChunkLength = Math.max(prefetchedChunkLength, 0);
        this.tailCaptureBuffer = new TailCaptureBuffer(tailCaptureBytes);
        this.successCallback = successCallback;
        this.errorCallback = errorCallback;
        this.cleanupCallback = cleanupCallback;
    }

    public ForwardingAsyncStreamBody(ProviderStreamHandle streamHandle,
                                     int tailCaptureBytes,
                                     Consumer<String> successCallback,
                                     Consumer<String> errorCallback,
                                     Runnable cleanupCallback) {
        this(streamHandle, null, 0, tailCaptureBytes, successCallback, errorCallback, cleanupCallback);
    }

    public int read(byte[] buffer) throws IOException {
        int prefetchedRead = readPrefetchedChunk(buffer);
        if (prefetchedRead >= 0) {
            return prefetchedRead;
        }
        int read = streamHandle.read(buffer);
        if (read > 0) {
            tailCaptureBuffer.append(buffer, 0, read);
        }
        return read;
    }

    public void completeSuccess() {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        try {
            successCallback.accept(tailCaptureBuffer.asUtf8());
        } finally {
            cleanup();
        }
    }

    public void completeError(String message) {
        if (!finished.compareAndSet(false, true)) {
            return;
        }
        try {
            errorCallback.accept(message);
        } finally {
            cleanup();
        }
    }

    public void abort(String message) {
        completeError(message);
    }

    public void closeUpstreamQuietly() {
        streamHandle.closeQuietly();
    }

    /**
     * 首包在执行层已经同步读取完成，这里优先把预读的数据交给下游，再继续读取真实上游流。
     */
    private int readPrefetchedChunk(byte[] buffer) {
        if (prefetchedOffset >= prefetchedChunkLength) {
            return -1;
        }
        int length = Math.min(buffer.length, prefetchedChunkLength - prefetchedOffset);
        System.arraycopy(prefetchedChunk, prefetchedOffset, buffer, 0, length);
        prefetchedOffset += length;
        tailCaptureBuffer.append(buffer, 0, length);
        return length;
    }

    private void cleanup() {
        try {
            streamHandle.closeQuietly();
        } finally {
            cleanupCallback.run();
        }
    }

    private static final class TailCaptureBuffer {

        private final byte[] buffer;
        private int size;

        private TailCaptureBuffer(int capacity) {
            this.buffer = new byte[Math.max(capacity, 1024)];
        }

        private void append(byte[] source, int off, int len) {
            if (len <= 0) {
                return;
            }
            if (len >= buffer.length) {
                System.arraycopy(source, off + len - buffer.length, buffer, 0, buffer.length);
                size = buffer.length;
                return;
            }
            if (size + len <= buffer.length) {
                System.arraycopy(source, off, buffer, size, len);
                size += len;
                return;
            }
            int overflow = size + len - buffer.length;
            System.arraycopy(buffer, overflow, buffer, 0, size - overflow);
            System.arraycopy(source, off, buffer, size - overflow, len);
            size = buffer.length;
        }

        private String asUtf8() {
            return new String(buffer, 0, size, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
