package site.xlinks.ai.router.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 支付宝支付请求DTO
 * 
 * @author xlinks
 */
@Data
@Schema(description = "支付宝支付请求参数")
public class AlipayPayRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过64位")
    @Schema(description = "商户订单号", example = "202404060001")
    private String outTradeNo;

    @NotNull(message = "支付金额不能为空")
    @Positive(message = "支付金额必须大于0")
    @Schema(description = "支付金额", example = "0.01")
    private Double totalAmount;

    @NotBlank(message = "订单标题不能为空")
    @Size(max = 256, message = "订单标题长度不能超过256位")
    @Schema(description = "订单标题", example = "测试商品")
    private String subject;

    @Size(max = 400, message = "订单描述长度不能超过400位")
    @Schema(description = "订单描述", example = "这是一个测试商品")
    private String body;
}
