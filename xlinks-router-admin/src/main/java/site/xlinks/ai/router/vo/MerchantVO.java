package site.xlinks.ai.router.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    private List<MerchantProviderRouteVO> providerRoutes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
