package site.xlinks.ai.router.service;

/**
 * Stream request didn't receive the first event in time.
 */
public class StreamFirstResponseTimeoutException extends RuntimeException {

    public StreamFirstResponseTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
