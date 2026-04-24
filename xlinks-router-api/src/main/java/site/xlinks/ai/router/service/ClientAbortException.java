package site.xlinks.ai.router.service;

/**
 * Raised when downstream client connection is already broken.
 */
public class ClientAbortException extends RuntimeException {

    public ClientAbortException(String message, Throwable cause) {
        super(message, cause);
    }
}
