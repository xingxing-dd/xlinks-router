package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Activation code view object.
 */
@Data
@Schema(description = "Activation code view object")
public class ActivationCodeVO {

    private Long id;

    private String activationCode;

    private Long planId;

    private String planName;

    private Integer status;

    private LocalDateTime usedAt;

    private Long usedBy;

    private String usedAccount;

    private Long subscriptionId;

    private String orderId;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
