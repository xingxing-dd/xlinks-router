package site.xlinks.ai.router.service;

/**
 * Non-stream upstream timeout.
 */
public class UpstreamTimeoutException extends RuntimeException {

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
