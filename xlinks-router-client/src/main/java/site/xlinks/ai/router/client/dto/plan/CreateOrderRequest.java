package site.xlinks.ai.router.client.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "type不能为空")
    private String type;

    private String planId;

    @NotNull(message = "amount不能为空")
    private BigDecimal amount;

    @NotBlank(message = "paymentMethod不能为空")
    private String paymentMethod;
}
