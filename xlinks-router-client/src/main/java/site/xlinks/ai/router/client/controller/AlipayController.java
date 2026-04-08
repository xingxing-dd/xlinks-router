package site.xlinks.ai.router.client.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.client.dto.AlipayQueryRequest;
import site.xlinks.ai.router.client.dto.AlipayRefundRequest;
import site.xlinks.ai.router.client.dto.ApiResponse;
import site.xlinks.ai.router.client.payment.utils.AlipaySignatureUtil;
import site.xlinks.ai.router.client.service.OrderFulfillmentService;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Alipay callback/query/refund controller.
 */
@Slf4j
@RestController
@RequestMapping("/alipay")
@RequiredArgsConstructor
@Tag(name = "Alipay", description = "Alipay integration endpoints")
public class AlipayController {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;
    private final CustomerOrderMapper customerOrderMapper;
    private final OrderFulfillmentService orderFulfillmentService;

    @GetMapping("/return")
    @Operation(summary = "Alipay return callback")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, String> params = getCallbackParams(request);
            boolean signVerified = AlipaySignatureUtil.rsaCheckV1(params, getAlipayPublicKey(), "UTF-8", "RSA2");
            if (!signVerified) {
                response.sendRedirect(alipayConfig.getPaymentErrorUrl() + "?msg=" + encode("signature verify failed"));
                return;
            }

            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            updateOrderByChannelStatus(outTradeNo, tradeNo, tradeStatus);
            response.sendRedirect(alipayConfig.getPaymentSuccessUrl() + "?orderNo=" + encode(outTradeNo));
        } catch (Exception e) {
            log.error("Failed to process Alipay return callback", e);
            response.sendRedirect(alipayConfig.getPaymentErrorUrl() + "?msg=" + encode("callback failed"));
        }
    }

    @PostMapping("/notify")
    @Operation(summary = "Alipay async notify callback")
    public void notifyUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String, String> params = getCallbackParams(request);
            boolean signVerified = AlipaySignatureUtil.rsaCheckV1(params, getAlipayPublicKey(), "UTF-8", "RSA2");

            PrintWriter out = response.getWriter();
            if (!signVerified) {
                out.println("fail");
                return;
            }

            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            updateOrderByChannelStatus(outTradeNo, tradeNo, tradeStatus);
            out.println("success");
        } catch (Exception e) {
            log.error("Failed to process Alipay notify callback", e);
            response.getWriter().println("fail");
        }
    }

    @PostMapping("/query")
    @Operation(summary = "Query Alipay order status")
    public ResponseEntity<ApiResponse<AlipayTradeQueryResponse>> queryOrder(@RequestBody AlipayQueryRequest request) {
        try {
            AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(request.getOutTradeNo());
            if (request.getTradeNo() != null) {
                model.setTradeNo(request.getTradeNo());
            }
            alipayRequest.setBizModel(model);

            AlipayTradeQueryResponse response = alipayClient.execute(alipayRequest);
            if (response.isSuccess()) {
                return ResponseEntity.ok(ApiResponse.success(response));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Order query failed: " + response.getSubMsg()));
        } catch (AlipayApiException e) {
            log.error("Alipay order query failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Order query exception: " + e.getMessage()));
        }
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund Alipay order")
    public ResponseEntity<ApiResponse<AlipayTradeRefundResponse>> refundOrder(@RequestBody AlipayRefundRequest request) {
        try {
            AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(request.getOutTradeNo());
            model.setRefundAmount(request.getRefundAmount().toString());
            model.setRefundReason(request.getRefundReason());
            model.setOutRequestNo(request.getOutRequestNo());
            alipayRequest.setBizModel(model);

            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
            if (response.isSuccess()) {
                markOrderRefunded(request.getOutTradeNo(), response.getTradeNo());
                return ResponseEntity.ok(ApiResponse.success(response));
            }
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Refund failed: " + response.getSubMsg()));
        } catch (AlipayApiException e) {
            log.error("Alipay refund failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Refund exception: " + e.getMessage()));
        }
    }

    private void updateOrderByChannelStatus(String orderNo,
                                            String refNo,
                                            String tradeStatus) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }

        CustomerOrder currentOrder = customerOrderMapper.selectOne(
                new LambdaQueryWrapper<CustomerOrder>()
                        .eq(CustomerOrder::getOrderNo, orderNo)
                        .last("limit 1")
        );
        if (currentOrder == null) {
            return;
        }
        Integer targetStatus = resolveOrderStatus(tradeStatus);
        Integer currentStatus = currentOrder.getStatus() == null ? 0 : currentOrder.getStatus();
        if (currentStatus != 0) {
            if (currentStatus == 1 && targetStatus != null && targetStatus == 1) {
                orderFulfillmentService.handlePaidOrder(orderNo);
            }
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (currentOrder.getExpiredAt() != null && !currentOrder.getExpiredAt().isAfter(now)) {
            closeExpiredPendingOrder(orderNo, now);
            return;
        }

        CustomerOrder update = new CustomerOrder();
        update.setRefNo(refNo);
        update.setStatus(targetStatus);
        if (update.getStatus() != null && (update.getStatus() == 1 || update.getStatus() == 4)) {
            update.setCompleteAt(now);
        }

        int affected = customerOrderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo)
                .eq(CustomerOrder::getStatus, 0));
        if (affected > 0 && targetStatus != null && targetStatus == 1) {
            orderFulfillmentService.handlePaidOrder(orderNo);
        }
    }

    private void markOrderRefunded(String orderNo, String refNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        CustomerOrder update = new CustomerOrder();
        update.setRefNo(refNo);
        update.setStatus(4);
        update.setCompleteAt(LocalDateTime.now());
        customerOrderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo));
    }

    private Integer resolveOrderStatus(String tradeStatus) {
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            return 1;
        }
        if ("TRADE_CLOSED".equals(tradeStatus)) {
            return 3;
        }
        return 2;
    }

    private void closeExpiredPendingOrder(String orderNo, LocalDateTime now) {
        CustomerOrder update = new CustomerOrder();
        update.setStatus(3);
        update.setRemark("订单已过期自动关闭");
        update.setUpdatedAt(now);
        customerOrderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo)
                .eq(CustomerOrder::getStatus, 0));
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private Map<String, String> getCallbackParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private String getAlipayPublicKey() {
        Object config = alipayConfig.getCurrentConfig();
        if (config instanceof AlipayConfig.SandboxConfig sandboxConfig) {
            return sandboxConfig.getAlipayPublicKey();
        }
        return ((AlipayConfig.ProductionConfig) config).getAlipayPublicKey();
    }
}

