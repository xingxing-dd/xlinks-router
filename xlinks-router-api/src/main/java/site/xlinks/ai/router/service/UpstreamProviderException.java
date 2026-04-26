package site.xlinks.ai.router.service;

/**
 * Represents an HTTP failure returned directly by the upstream provider.
 */
public class UpstreamProviderException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public UpstreamProviderException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public boolean isRetryable() {
        return statusCode >= 500 && statusCode < 600;
    }
}
