package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Wallet amount operation request")
public class WalletAmountOperationDTO {

    @NotNull
    @DecimalMin(value = "0.01")
    @Schema(description = "Amount", required = true)
    private BigDecimal amount;

    @Schema(description = "Business order number")
    private String orderNo;

    @Schema(description = "Remark")
    private String remark;
}
