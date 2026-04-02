package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin account profile.
 */
@Data
@Schema(description = "Admin account profile")
public class AdminAccountProfileVO {

    @Schema(description = "Admin account ID")
    private Long id;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Phone")
    private String phone;

    @Schema(description = "Status")
    private Integer status;

    @Schema(description = "Last login time")
    private LocalDateTime lastLoginAt;
}
