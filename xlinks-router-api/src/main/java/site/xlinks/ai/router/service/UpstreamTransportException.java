package site.xlinks.ai.router.service;

/**
 * Represents network or transport-level failures while talking to upstream.
 */
public class UpstreamTransportException extends RuntimeException {

    public UpstreamTransportException(String message, Throwable cause) {
        super(message, cause);
    }
}
