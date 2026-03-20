package site.xlinks.ai.router.client.dto.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "planId不能为空")
    private String planId;

    @NotBlank(message = "paymentMethod不能为空")
    private String paymentMethod;

}
