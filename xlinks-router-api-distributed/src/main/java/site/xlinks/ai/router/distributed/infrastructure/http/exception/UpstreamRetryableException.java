package site.xlinks.ai.router.distributed.infrastructure.http.exception;

import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
public class UpstreamRetryableException extends RuntimeException {

    private final transient ResponseEntity<String> responseEntity;

    public UpstreamRetryableException(String message) {
        super(message);
        this.responseEntity = null;
    }

    public UpstreamRetryableException(String message, Throwable cause) {
        super(message, cause);
        this.responseEntity = null;
    }

    public UpstreamRetryableException(String message, ResponseEntity<String> responseEntity) {
        super(message);
        this.responseEntity = responseEntity;
    }
}
