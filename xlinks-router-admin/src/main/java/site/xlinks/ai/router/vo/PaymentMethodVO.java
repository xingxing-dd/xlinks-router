package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payment method view object")
public class PaymentMethodVO {

    private Long id;

    private String methodCode;

    private String methodName;

    private String methodType;

    private String iconUrl;

    private Integer sort;

    private Integer status;

    private String configJson;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
