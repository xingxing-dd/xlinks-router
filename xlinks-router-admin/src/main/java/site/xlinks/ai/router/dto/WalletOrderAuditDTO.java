package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Wallet order audit request")
public class WalletOrderAuditDTO {

    @Schema(description = "Remark")
    private String remark;
}
