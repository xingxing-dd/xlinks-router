package site.xlinks.ai.router.merchant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * RSA 公钥响应。
 */
@Data
@Builder
public class RsaPublicKeyVO {

    @Schema(description = "RSA 算法")
    private String algorithm;

    @Schema(description = "RSA 公钥，Base64 编码")
    private String publicKey;
}