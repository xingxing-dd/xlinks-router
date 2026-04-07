package site.xlinks.ai.router.client.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝配置类
 * 
 * @author xlinks
 */
@Data
@Slf4j
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /**
     * 沙箱环境配置
     */
    private SandboxConfig sandbox;

    /**
     * 正式环境配置
     */
    private ProductionConfig production;

    @Data
    public static class SandboxConfig {
        /**
         * 是否启用沙箱环境
         */
        private boolean enabled = true;
        
        /**
         * 应用ID
         */
        private String appId;
        
        /**
         * 商户私钥
         */
        private String privateKey;
        
        /**
         * 商户公钥
         */
        private String publicKey;
        
        /**
         * 支付宝公钥
         */
        private String alipayPublicKey;
        
        /**
         * 支付宝网关地址
         */
        private String gatewayUrl;
        
        /**
         * 字符编码
         */
        private String charset = "UTF-8";
        
        /**
         * 签名算法类型
         */
        private String signType = "RSA2";
        
        /**
         * 数据格式
         */
        private String format = "json";
        
        /**
         * 同步回调地址
         */
        private String returnUrl;
        
        /**
         * 异步通知地址
         */
        private String notifyUrl;

        /**
         * 前端支付成功页地址
         */
        private String paymentSuccessUrl;

        /**
         * 前端支付失败页地址
         */
        private String paymentErrorUrl;
    }

    @Data
    public static class ProductionConfig {
        /**
         * 是否启用正式环境
         */
        private boolean enabled = false;
        
        /**
         * 应用ID
         */
        private String appId;
        
        /**
         * 商户私钥
         */
        private String privateKey;
        
        /**
         * 商户公钥
         */
        private String publicKey;
        
        /**
         * 支付宝公钥
         */
        private String alipayPublicKey;
        
        /**
         * 支付宝网关地址
         */
        private String gatewayUrl;
        
        /**
         * 字符编码
         */
        private String charset = "UTF-8";
        
        /**
         * 签名算法类型
         */
        private String signType = "RSA2";
        
        /**
         * 数据格式
         */
        private String format = "json";
        
        /**
         * 同步回调地址
         */
        private String returnUrl;
        
        /**
         * 异步通知地址
         */
        private String notifyUrl;

        /**
         * 前端支付成功页地址
         */
        private String paymentSuccessUrl;

        /**
         * 前端支付失败页地址
         */
        private String paymentErrorUrl;
    }

    /**
     * 创建支付宝客户端
     * 优先使用沙箱环境配置
     * 
     * @return AlipayClient
     */
    @Bean
    public AlipayClient alipayClient() {
        boolean useSandbox = sandbox != null && sandbox.isEnabled();
        boolean useProduction = production != null && production.isEnabled();
        
        if (!useSandbox && !useProduction) {
            throw new IllegalStateException("支付宝配置未启用，请启用沙箱环境或正式环境配置");
        }
        
        // 优先使用沙箱环境
        if (useSandbox) {
            log.info("使用支付宝沙箱环境配置");
            return createAlipayClient(sandbox);
        } else {
            log.info("使用支付宝正式环境配置");
            return createAlipayClient(production);
        }
    }

    /**
     * 根据配置创建支付宝客户端
     * 
     * @param config 配置对象
     * @return AlipayClient
     */
    private AlipayClient createAlipayClient(Object config) {
        String appId, privateKey, alipayPublicKey, gatewayUrl;
        String charset = "UTF-8";
        String signType = "RSA2";
        String format = "json";
        
        if (config instanceof SandboxConfig) {
            SandboxConfig sandboxConfig = (SandboxConfig) config;
            appId = sandboxConfig.getAppId();
            privateKey = sandboxConfig.getPrivateKey();
            alipayPublicKey = sandboxConfig.getAlipayPublicKey();
            gatewayUrl = sandboxConfig.getGatewayUrl();
            charset = sandboxConfig.getCharset();
            signType = sandboxConfig.getSignType();
            format = sandboxConfig.getFormat();
        } else {
            ProductionConfig productionConfig = (ProductionConfig) config;
            appId = productionConfig.getAppId();
            privateKey = productionConfig.getPrivateKey();
            alipayPublicKey = productionConfig.getAlipayPublicKey();
            gatewayUrl = productionConfig.getGatewayUrl();
            charset = productionConfig.getCharset();
            signType = productionConfig.getSignType();
            format = productionConfig.getFormat();
        }
        
        // 验证必要参数
        if (appId == null || appId.trim().isEmpty()) {
            throw new IllegalArgumentException("支付宝应用ID不能为空");
        }
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new IllegalArgumentException("支付宝商户私钥不能为空");
        }
        if (alipayPublicKey == null || alipayPublicKey.trim().isEmpty()) {
            throw new IllegalArgumentException("支付宝公钥不能为空");
        }
        if (gatewayUrl == null || gatewayUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("支付宝网关地址不能为空");
        }
        
        log.info("创建支付宝客户端: appId={}, gatewayUrl={}, signType={}", appId, gatewayUrl, signType);
        
        return new DefaultAlipayClient(
            gatewayUrl,
            appId,
            privateKey,
            format,
            charset,
            alipayPublicKey,
            signType
        );
    }

    /**
     * 获取当前使用的配置
     * 
     * @return 配置对象
     */
    public Object getCurrentConfig() {
        if (sandbox != null && sandbox.isEnabled()) {
            return sandbox;
        } else if (production != null && production.isEnabled()) {
            return production;
        }
        throw new IllegalStateException("支付宝配置未启用");
    }

    /**
     * 获取同步回调地址
     * 
     * @return 回调地址
     */
    public String getReturnUrl() {
        Object config = getCurrentConfig();
        if (config instanceof SandboxConfig) {
            return ((SandboxConfig) config).getReturnUrl();
        } else {
            return ((ProductionConfig) config).getReturnUrl();
        }
    }

    /**
     * 获取异步通知地址
     * 
     * @return 通知地址
     */
    public String getNotifyUrl() {
        Object config = getCurrentConfig();
        if (config instanceof SandboxConfig) {
            return ((SandboxConfig) config).getNotifyUrl();
        } else {
            return ((ProductionConfig) config).getNotifyUrl();
        }
    }

    /**
     * 获取前端支付成功页地址
     *
     * @return 成功页地址
     */
    public String getPaymentSuccessUrl() {
        Object config = getCurrentConfig();
        if (config instanceof SandboxConfig) {
            return ((SandboxConfig) config).getPaymentSuccessUrl();
        } else {
            return ((ProductionConfig) config).getPaymentSuccessUrl();
        }
    }

    /**
     * 获取前端支付失败页地址
     *
     * @return 失败页地址
     */
    public String getPaymentErrorUrl() {
        Object config = getCurrentConfig();
        if (config instanceof SandboxConfig) {
            return ((SandboxConfig) config).getPaymentErrorUrl();
        } else {
            return ((ProductionConfig) config).getPaymentErrorUrl();
        }
    }
}
