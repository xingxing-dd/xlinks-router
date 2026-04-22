package site.xlinks.ai.router.client.dto.wallet;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithdrawOrderResponse {
    private String orderId;
    private String status;
}
