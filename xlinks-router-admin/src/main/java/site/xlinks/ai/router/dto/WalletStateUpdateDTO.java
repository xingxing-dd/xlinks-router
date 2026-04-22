package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Wallet state update request")
public class WalletStateUpdateDTO {

    @Schema(description = "Allow incoming: 1 yes, 0 no")
    private Integer allowIn;

    @Schema(description = "Allow outgoing: 1 yes, 0 no")
    private Integer allowOut;

    @Schema(description = "Wallet status: 1 enabled, 0 disabled")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;
}
