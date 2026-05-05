package site.xlinks.ai.router.infrastructure.http.exception;

public class UpstreamTimeoutException extends UpstreamRetryableException {

    public UpstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
