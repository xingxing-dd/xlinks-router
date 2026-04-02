package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Admin login response.
 */
@Data
@Schema(description = "Admin login response")
public class AdminLoginVO {

    @Schema(description = "Bearer token")
    private String accessToken;

    @Schema(description = "Token type")
    private String tokenType;

    @Schema(description = "Expire seconds")
    private Long expiresIn;

    @Schema(description = "Admin profile")
    private AdminAccountProfileVO user;
}
