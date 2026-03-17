package site.xlinks.ai.router.client.dto.plan;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private String orderId;
    private String payUrl;
    private String expireTime;
}
