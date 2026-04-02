package site.xlinks.ai.router.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayLinkUpdateDTO {

    @Size(max = 500, message = "Pay URL length must not exceed 500")
    private String payUrl;

    private Integer status;

    @Size(max = 500, message = "Remark length must not exceed 500")
    private String remark;
}
