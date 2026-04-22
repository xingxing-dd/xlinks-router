package site.xlinks.ai.router.client.dto.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletWithdrawOrderRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private String remark;
}
