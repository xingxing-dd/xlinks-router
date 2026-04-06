package site.xlinks.ai.router.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Size;
import java.io.Serializable;

/**
 * 支付宝查询请求DTO
 * 
 * @author xlinks
 */
@Data
@Schema(description = "支付宝查询请求参数")
public class AlipayQueryRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 64, message = "商户订单号长度不能超过64位")
    @Schema(description = "商户订单号", example = "202404060001")
    private String outTradeNo;

    @Size(max = 64, message = "支付宝交易号长度不能超过64位")
    @Schema(description = "支付宝交易号", example = "2024040622001234567890123456")
    private String tradeNo;
}
