package site.xlinks.ai.router.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PayLinkCreateDTO {

    @NotNull(message = "Target plan is required")
    private Long targetId;

    @NotBlank(message = "Pay URL is required")
    @Size(max = 500, message = "Pay URL length must not exceed 500")
    private String payUrl;

    private Integer status;

    @Size(max = 500, message = "Remark length must not exceed 500")
    private String remark;
}
