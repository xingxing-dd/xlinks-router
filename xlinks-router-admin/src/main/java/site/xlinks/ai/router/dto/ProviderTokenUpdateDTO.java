package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Provider Token 更新 DTO
 */
@Data
@Schema(description = "Provider Token 更新请求")
public class ProviderTokenUpdateDTO {

    @Schema(description = "Token 名称")
    private String tokenName;

    @Schema(description = "Token 值")
    private String tokenValue;

    @Schema(description = "配额总量")
    private Long quotaTotal;

    @Schema(description = "过期时间")
    private java.time.LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
