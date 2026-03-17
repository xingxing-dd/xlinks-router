package site.xlinks.ai.router.client.dto.token;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateCustomerTokenRequest {
    @NotBlank(message = "tokenName不能为空")
    private String tokenName;

    private List<String> allowedModels;

    @NotNull(message = "expireDays不能为空")
    private Integer expireDays;
}
