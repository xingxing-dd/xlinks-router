package site.xlinks.ai.router.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentMethodUpdateDTO {

    @Size(max = 100, message = "Method name length must not exceed 100")
    private String methodName;

    @Size(max = 50, message = "Method type length must not exceed 50")
    private String methodType;

    @Size(max = 255, message = "Icon URL length must not exceed 255")
    private String iconUrl;

    private Integer sort;

    private Integer status;

    private String configJson;

    @Size(max = 500, message = "Remark length must not exceed 500")
    private String remark;
}
