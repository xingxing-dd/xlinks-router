package site.xlinks.ai.router.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Customer Token 创建 DTO
 */
@Data
@Schema(description = "Customer Token 创建请求")
public class CustomerTokenCreateDTO {

    @Schema(description = "客户名称", required = true)
    @NotBlank(message = "客户名称不能为空")
    private String customerName;

    @Schema(description = "Token 名称", required = true)
    @NotBlank(message = "Token 名称不能为空")
    private String tokenName;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status = 1;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "允许访问的模型列表（JSON 数组）")
    private String allowedModels;

    @Schema(description = "备注")
    private String remark;
}
