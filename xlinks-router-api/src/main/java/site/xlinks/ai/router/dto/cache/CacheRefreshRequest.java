package site.xlinks.ai.router.dto.cache;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Internal cache refresh request.
 */
@Data
public class CacheRefreshRequest {

    @NotBlank(message = "type cannot be blank")
    private String type;

    @NotBlank(message = "action cannot be blank")
    private String action;

    private Long id;

    private Long accountId;

    private Long providerId;

    private Long modelId;

    private Long planId;

    private String source;

    private String remark;
}
