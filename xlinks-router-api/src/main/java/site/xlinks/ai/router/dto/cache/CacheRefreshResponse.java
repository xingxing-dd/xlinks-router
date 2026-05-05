package site.xlinks.ai.router.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 内部缓存刷新响应。
 */
@Data
@AllArgsConstructor
public class CacheRefreshResponse {

    private String type;

    private String action;

    private String mode;

    private String scope;

    private String message;

    private String timestamp;
}
