package site.xlinks.ai.router.service;

/**
 * Stream request stalled after at least one event was received.
 */
public class StreamIdleTimeoutException extends RuntimeException {

    public StreamIdleTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
