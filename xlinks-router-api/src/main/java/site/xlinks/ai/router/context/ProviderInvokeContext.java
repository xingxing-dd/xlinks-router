package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

/**
 * Provider 璋冪敤涓婁笅鏂?
 * 鍖呭惈璋冪敤涓嬫父 Provider 鎵€闇€鐨勫畬鏁翠俊鎭?
 */
@Data
@Builder
public class ProviderInvokeContext {

    /**
     * 璇锋眰 ID
     */
    private String requestId;

    /**
     * Provider ID
     */
    private Long providerId;

    /**
     * Provider 缂栫爜
     */
    private String providerCode;

    /**
     * Provider 鍚嶇О
     */
    private String providerName;


    /**
     * 鍩虹 URL
     */
    private String baseUrl;

    /**
     * Provider Token
     */
    private String providerToken;

    /**
     * 瀹㈡埛 Token
     */
    private String customerToken;

    /**
     * 绔偣缂栫爜
     */
    private String endpointCode;

    /**
     * 妯″瀷 ID
     */
    private Long modelId;

    /**
     * 妯″瀷缂栫爜
     */
    private String modelCode;

    /**
     * 妯″瀷鍚嶇О
     */
    private String modelName;

    /**
     * 杈撳叆浠锋牸锛堟瘡鐧句竾 token锛?
     */
    private java.math.BigDecimal inputPrice;

    /**
     * 杈撳嚭浠锋牸锛堟瘡鐧句竾 token锛?
     */
    private java.math.BigDecimal outputPrice;

    /**
     * 搴曞眰妯″瀷鍚嶇О
     */
    private String providerModel;

    /**
     * 濂楅璁板綍 ID
     */
    private Long planId;

    /**
     * 瀹㈡埛 Token ID
     */
    private Long customerTokenId;

    /**
     * 瀹㈡埛璐︽埛 ID
     */
    private Long accountId;

    /**
     * 瀹㈡埛妯″瀷缂栫爜
     */
    private String customerModel;
}
