package site.xlinks.ai.router.client.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.xlinks.ai.router.client.config.AlipayConfig;
import site.xlinks.ai.router.client.dto.AlipayPayRequest;
import site.xlinks.ai.router.client.dto.AlipayQueryRequest;
import site.xlinks.ai.router.client.dto.AlipayRefundRequest;
import site.xlinks.ai.router.client.dto.ApiResponse;
import site.xlinks.ai.router.client.payment.utils.AlipaySignatureUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝支付控制器
 * 
 * @author xlinks
 */
@Slf4j
@RestController
@RequestMapping("/alipay")
@RequiredArgsConstructor
@Tag(name = "支付宝支付", description = "支付宝支付相关接口")
public class AlipayController {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    /**
     * 电脑网站支付
     * 
     * @param request 支付请求参数
     * @return 支付页面URL
     */
    @PostMapping("/pay")
    @Operation(summary = "电脑网站支付", description = "创建支付宝电脑网站支付订单")
    public ResponseEntity<ApiResponse<String>> pagePay(@RequestBody AlipayPayRequest request) {
        try {
            log.info("开始创建支付宝支付订单: {}", request.getOutTradeNo());

            // 创建支付请求
            AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
            
            // 设置回调地址
            alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
            alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());

            // 设置业务参数
            AlipayTradePagePayModel model = new AlipayTradePagePayModel();
            model.setOutTradeNo(request.getOutTradeNo());
            model.setTotalAmount(request.getTotalAmount().toString());
            model.setSubject(request.getSubject());
            model.setBody(request.getBody());
            model.setProductCode("FAST_INSTANT_TRADE_PAY");
            
            alipayRequest.setBizModel(model);

            // 调用支付宝API
            AlipayTradePagePayResponse response = alipayClient.pageExecute(alipayRequest);
            
            if (response.isSuccess()) {
                log.info("支付宝支付订单创建成功: {}", response.getBody());
                return ResponseEntity.ok(ApiResponse.success(response.getBody()));
            } else {
                log.error("支付宝支付订单创建失败: {}", response.getSubMsg());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("支付订单创建失败: " + response.getSubMsg()));
            }

        } catch (AlipayApiException e) {
            log.error("支付宝支付订单创建异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("支付订单创建异常: " + e.getMessage()));
        }
    }

    /**
     * 支付同步回调
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @GetMapping("/return")
    @Operation(summary = "支付同步回调", description = "支付宝支付同步回调处理")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("收到支付宝同步回调");
        
        try {
            // 获取回调参数
            Map<String, String> params = getCallbackParams(request);
            
            // 验证签名
            boolean signVerified = AlipaySignatureUtil.rsaCheckV1(params, 
                getAlipayPublicKey(), "UTF-8", "RSA2");
            
            if (signVerified) {
                log.info("支付宝同步回调签名验证成功");
                
                // 处理业务逻辑
                String outTradeNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                String tradeStatus = params.get("trade_status");
                
                log.info("订单信息: outTradeNo={}, tradeNo={}, tradeStatus={}", 
                    outTradeNo, tradeNo, tradeStatus);
                
                // 处理业务逻辑：根据支付状态进行相应处理
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付成功，可以进行页面跳转前的业务处理
                    log.info("用户支付完成，跳转到成功页面: {}", outTradeNo);
                    // TODO: 可以在这里进行一些同步的业务处理
                    // 注意：重要的业务逻辑应该在异步通知中处理，避免用户关闭页面导致处理中断
                } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                    // 等待付款
                    log.info("订单等待付款: {}", outTradeNo);
                } else {
                    // 其他状态
                    log.info("订单状态: {}, 订单号: {}", tradeStatus, outTradeNo);
                }
                
                // 重定向到成功页面
                response.sendRedirect("/payment/success?orderNo=" + outTradeNo);
                
            } else {
                log.error("支付宝同步回调签名验证失败");
                response.sendRedirect("/payment/error?msg=签名验证失败");
            }
            
        } catch (Exception e) {
            log.error("支付宝同步回调处理异常", e);
            response.sendRedirect("/payment/error?msg=回调处理异常");
        }
    }

    /**
     * 支付异步通知
     * 
     * @param request HTTP请求
     * @param response HTTP响应
     */
    @PostMapping("/notify")
    @Operation(summary = "支付异步通知", description = "支付宝支付异步通知处理")
    public void notifyUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("收到支付宝异步通知");
        
        try {
            // 获取通知参数
            Map<String, String> params = getCallbackParams(request);
            
            // 验证签名
            boolean signVerified = AlipaySignatureUtil.rsaCheckV1(params, 
                getAlipayPublicKey(), "UTF-8", "RSA2");
            
            PrintWriter out = response.getWriter();
            
            if (signVerified) {
                log.info("支付宝异步通知签名验证成功");
                
                // 处理业务逻辑
                String outTradeNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                String tradeStatus = params.get("trade_status");
                String totalAmount = params.get("total_amount");
                
                log.info("异步通知订单信息: outTradeNo={}, tradeNo={}, tradeStatus={}, totalAmount={}", 
                    outTradeNo, tradeNo, tradeStatus, totalAmount);
                
                // 处理业务逻辑：更新订单状态
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    // 支付成功，更新订单状态
                    log.info("订单支付成功，更新订单状态: {}", outTradeNo);
                    // TODO: 调用订单服务更新订单状态
                    // orderService.updateOrderStatus(outTradeNo, "PAID", tradeNo);
                    
                    // TODO: 其他业务逻辑，如发货、增加积分等
                    // shipmentService.createShipment(outTradeNo);
                    // pointsService.addPoints(userId, totalAmount);
                } else if ("TRADE_CLOSED".equals(tradeStatus)) {
                    // 交易关闭
                    log.info("订单交易关闭: {}", outTradeNo);
                    // TODO: 更新订单状态为已关闭
                    // orderService.updateOrderStatus(outTradeNo, "CLOSED", tradeNo);
                }
                
                // 返回成功响应
                out.println("success");
                
            } else {
                log.error("支付宝异步通知签名验证失败");
                out.println("fail");
            }
            
        } catch (Exception e) {
            log.error("支付宝异步通知处理异常", e);
            response.getWriter().println("fail");
        }
    }

    /**
     * 订单查询
     * 
     * @param request 查询请求参数
     * @return 查询结果
     */
    @PostMapping("/query")
    @Operation(summary = "订单查询", description = "查询支付宝订单状态")
    public ResponseEntity<ApiResponse<AlipayTradeQueryResponse>> queryOrder(@RequestBody AlipayQueryRequest request) {
        try {
            log.info("开始查询支付宝订单: {}", request.getOutTradeNo());

            // 创建查询请求
            AlipayTradeQueryRequest alipayRequest = new AlipayTradeQueryRequest();
            
            // 设置业务参数
            AlipayTradeQueryModel model = new AlipayTradeQueryModel();
            model.setOutTradeNo(request.getOutTradeNo());
            // 如果有支付宝交易号，也可以使用
            if (request.getTradeNo() != null) {
                model.setTradeNo(request.getTradeNo());
            }
            
            alipayRequest.setBizModel(model);

            // 调用支付宝API
            AlipayTradeQueryResponse response = alipayClient.execute(alipayRequest);
            
            if (response.isSuccess()) {
                log.info("支付宝订单查询成功: {}", response.getTradeStatus());
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                log.error("支付宝订单查询失败: {}", response.getSubMsg());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("订单查询失败: " + response.getSubMsg()));
            }

        } catch (AlipayApiException e) {
            log.error("支付宝订单查询异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("订单查询异常: " + e.getMessage()));
        }
    }

    /**
     * 订单退款
     * 
     * @param request 退款请求参数
     * @return 退款结果
     */
    @PostMapping("/refund")
    @Operation(summary = "订单退款", description = "申请支付宝订单退款")
    public ResponseEntity<ApiResponse<AlipayTradeRefundResponse>> refundOrder(@RequestBody AlipayRefundRequest request) {
        try {
            log.info("开始申请支付宝退款: outTradeNo={}, refundAmount={}", 
                request.getOutTradeNo(), request.getRefundAmount());

            // 创建退款请求
            AlipayTradeRefundRequest alipayRequest = new AlipayTradeRefundRequest();
            
            // 设置业务参数
            AlipayTradeRefundModel model = new AlipayTradeRefundModel();
            model.setOutTradeNo(request.getOutTradeNo());
            model.setRefundAmount(request.getRefundAmount().toString());
            model.setRefundReason(request.getRefundReason());
            model.setOutRequestNo(request.getOutRequestNo());
            
            alipayRequest.setBizModel(model);

            // 调用支付宝API
            AlipayTradeRefundResponse response = alipayClient.execute(alipayRequest);
            
            if (response.isSuccess()) {
                log.info("支付宝退款申请成功: {}", response.getRefundFee());
                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                log.error("支付宝退款申请失败: {}", response.getSubMsg());
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("退款申请失败: " + response.getSubMsg()));
            }

        } catch (AlipayApiException e) {
            log.error("支付宝退款申请异常", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("退款申请异常: " + e.getMessage()));
        }
    }

    /**
     * 获取回调参数
     * 
     * @param request HTTP请求
     * @return 参数Map
     */
    private Map<String, String> getCallbackParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    /**
     * 获取支付宝公钥
     * 
     * @return 支付宝公钥
     */
    private String getAlipayPublicKey() {
        Object config = alipayConfig.getCurrentConfig();
        if (config instanceof AlipayConfig.SandboxConfig) {
            return ((AlipayConfig.SandboxConfig) config).getAlipayPublicKey();
        } else {
            return ((AlipayConfig.ProductionConfig) config).getAlipayPublicKey();
        }
    }
}
