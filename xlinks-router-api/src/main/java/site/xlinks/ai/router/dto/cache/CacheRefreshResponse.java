package site.xlinks.ai.router.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal cache refresh response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheRefreshResponse {

    private String type;

    private String action;

    private String mode;

    private String scope;

    private String message;

    private String refreshedAt;
}
