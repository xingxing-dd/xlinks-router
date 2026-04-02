package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Admin login request.
 */
@Data
@Schema(description = "Admin login request")
public class AdminLoginDTO {

    @NotBlank(message = "Username must not be blank")
    @Schema(description = "Admin username", required = true)
    private String username;

    @NotBlank(message = "Password must not be blank")
    @Schema(description = "Admin password", required = true)
    private String password;
}
