package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Customer Token 更新 DTO
 */
@Data
@Schema(description = "Customer Token 更新请求")
public class CustomerTokenUpdateDTO {

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "Token 名称")
    private String tokenName;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "允许访问的模型列表")
    private String allowedModels;

    @Schema(description = "备注")
    private String remark;
}
