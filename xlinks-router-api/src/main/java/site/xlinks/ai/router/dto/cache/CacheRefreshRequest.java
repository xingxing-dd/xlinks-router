package site.xlinks.ai.router.dto.cache;

import lombok.Data;

/**
 * 内部缓存刷新请求。
 */
@Data
public class CacheRefreshRequest {

    private String source;

    private String type;

    private String action;

    private Long id;

    private Long accountId;

    private Long providerId;

    private Long modelId;

    private Long planId;

    private String remark;
}
