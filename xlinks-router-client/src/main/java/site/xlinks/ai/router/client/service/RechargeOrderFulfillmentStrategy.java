package site.xlinks.ai.router.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fulfillment strategy for recharge orders.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RechargeOrderFulfillmentStrategy implements OrderFulfillmentStrategy {

    private static final String ORDER_TYPE = WalletConstants.ORDER_TYPE_RECHARGE;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WalletService walletService;
    private final PromotionService promotionService;
    private final ObjectMapper objectMapper;

    @Override
    public String orderType() {
        return ORDER_TYPE;
    }

    @Override
    public OrderFulfillmentResult fulfill(CustomerOrder order) {
        if (order == null || order.getAccountId() == null) {
            return OrderFulfillmentResult.skipped(ORDER_TYPE, "recharge order is invalid");
        }

        BigDecimal rechargeAmount = resolveRechargeAmount(order);
        if (rechargeAmount == null || rechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return OrderFulfillmentResult.skipped(ORDER_TYPE, "recharge amount is invalid");
        }

        walletService.creditBasic(order.getAccountId(), rechargeAmount, WalletConstants.BIZ_TYPE_RECHARGE,
                order.getOrderNo(), "Wallet recharge completed");
        promotionService.createFirstRechargeReward(order.getAccountId(), rechargeAmount, order.getOrderNo());
        log.info("Recharge order fulfilled, orderNo={}, accountId={}, rechargeAmount={}, paidAmount={}",
                order.getOrderNo(), order.getAccountId(), rechargeAmount, order.getTotalAmount());
        return OrderFulfillmentResult.fulfilled(ORDER_TYPE, null, "wallet recharge completed");
    }

    private BigDecimal resolveRechargeAmount(CustomerOrder order) {
        BigDecimal snapshotAmount = parseRechargeAmount(order.getOrderInfo());
        return snapshotAmount != null ? snapshotAmount : order.getTotalAmount();
    }

    private BigDecimal parseRechargeAmount(String orderInfo) {
        if (orderInfo == null || orderInfo.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(orderInfo, MAP_TYPE);
            BigDecimal rechargeAmount = toBigDecimal(map.get("rechargeAmount"));
            return rechargeAmount != null ? rechargeAmount : toBigDecimal(map.get("amount"));
        } catch (Exception ex) {
            log.warn("Failed to parse recharge order info. orderInfo={}", orderInfo, ex);
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
