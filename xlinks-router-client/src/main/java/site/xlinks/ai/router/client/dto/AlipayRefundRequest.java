package site.xlinks.ai.router.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 支付宝退款请求DTO
 * 
 * @author xlinks
 */
@Data
@Schema(description = "支付宝退款请求参数")
public class AlipayRefundRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "商户订单号不能为空")
    @Size(max = 64, message = "商户订单号长度不能超过64位")
    @Schema(description = "商户订单号", example = "202404060001")
    private String outTradeNo;

    @NotNull(message = "退款金额不能为空")
    @Positive(message = "退款金额必须大于0")
    @Schema(description = "退款金额", example = "0.01")
    private Double refundAmount;

    @NotBlank(message = "退款请求号不能为空")
    @Size(max = 64, message = "退款请求号长度不能超过64位")
    @Schema(description = "退款请求号", example = "202404060001001")
    private String outRequestNo;

    @Size(max = 256, message = "退款原因长度不能超过256位")
    @Schema(description = "退款原因", example = "商品质量问题")
    private String refundReason;
}
