package site.xlinks.ai.router.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MerchantUpdateDTO {

    @Size(max = 500, message = "Remark length must not exceed 500")
    private String remark;
}
