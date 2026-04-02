package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Merchant view object")
public class MerchantVO {

    private Long id;

    private String username;

    private String phone;

    private String email;

    private String inviteCode;

    private Long invitedBy;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
