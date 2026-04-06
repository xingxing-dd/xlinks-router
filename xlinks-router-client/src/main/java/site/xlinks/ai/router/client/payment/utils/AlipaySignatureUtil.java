package site.xlinks.ai.router.client.payment.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RSA2 验签工具类
 * 用于支付宝异步通知验签
 * 
 * @author xlinks
 */
@Slf4j
public class AlipaySignatureUtil {

    /**
     * RSA2 签名验证
     * 
     * @param params 待验签参数Map
     * @param publicKey 支付宝公钥
     * @param charset 字符编码
     * @param signType 签名类型
     * @return 验签结果
     */
    public static boolean rsaCheckV1(Map<String, String> params, String publicKey, 
                                     String charset, String signType) {
        try {
            // 1. 获取签名
            String sign = params.get("sign");
            if (sign == null || sign.trim().isEmpty()) {
                log.error("签名为空");
                return false;
            }

            // 2. 获取签名内容
            String signContent = getSignCheckContent(params);
            if (signContent == null || signContent.trim().isEmpty()) {
                log.error("签名内容为空");
                return false;
            }

            log.info("开始RSA2验签: signType={}, charset={}", signType, charset);
            log.debug("签名内容: {}", signContent);

            // 3. 验证签名
            boolean result = rsaVerify(signContent, sign, publicKey, charset);

            log.info("RSA2验签结果: {}", result);
            return result;

        } catch (Exception e) {
            log.error("RSA2验签异常", e);
            return false;
        }
    }

    /**
     * RSA2 签名
     * 
     * @param content 待签名内容
     * @param privateKey 商户私钥
     * @param charset 字符编码
     * @param signType 签名类型
     * @return 签名结果
     */
    public static String rsaSign(String content, String privateKey, 
                                String charset, String signType) {
        try {
            PrivateKey priKey = getPrivateKey(privateKey);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(priKey);
            
            if (charset == null || charset.isEmpty()) {
                signature.update(content.getBytes());
            } else {
                signature.update(content.getBytes(charset));
            }
            
            byte[] signed = signature.sign();
            return base64Encode(signed);
            
        } catch (Exception e) {
            log.error("RSA2签名异常", e);
            return null;
        }
    }

    /**
     * RSA2 验证签名
     * 
     * @param content 待验证内容
     * @param sign 签名值
     * @param publicKey 公钥
     * @param charset 字符编码
     * @return 验证结果
     */
    public static boolean rsaVerify(String content, String sign, String publicKey, String charset) {
        try {
            PublicKey pubKey = getPublicKey(publicKey);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pubKey);
            
            if (charset == null || charset.isEmpty()) {
                signature.update(content.getBytes());
            } else {
                signature.update(content.getBytes(charset));
            }
            
            return signature.verify(base64Decode(sign));
            
        } catch (Exception e) {
            log.error("RSA2验证签名异常", e);
            return false;
        }
    }

    /**
     * 获取待验签的内容
     * 
     * @param params 参数Map
     * @return 待验签内容
     */
    private static String getSignCheckContent(Map<String, String> params) {
        try {
            // 过滤掉sign和sign_type参数
            List<String> keys = new ArrayList<>(params.keySet());
            keys.remove("sign");
            keys.remove("sign_type");
            
            // 排序
            Collections.sort(keys);
            
            // 构建待签名字符串
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String value = params.get(key);
                
                if (value != null && !value.trim().isEmpty()) {
                    if (i != 0) {
                        sb.append("&");
                    }
                    sb.append(key).append("=").append(value);
                }
            }
            
            return sb.toString();
            
        } catch (Exception e) {
            log.error("获取待验签内容异常", e);
            return null;
        }
    }

    /**
     * 获取私钥对象
     * 
     * @param privateKey 私钥字符串
     * @return PrivateKey
     */
    private static PrivateKey getPrivateKey(String privateKey) throws Exception {
        // 移除私钥中的头尾标记和换行符
        String privateKeyStr = privateKey.replace("-----BEGIN PRIVATE KEY-----", "")
                                         .replace("-----END PRIVATE KEY-----", "")
                                         .replace("\n", "")
                                         .replace("\r", "")
                                         .trim();
        
        byte[] keyBytes = base64Decode(privateKeyStr);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * 获取公钥对象
     * 
     * @param publicKey 公钥字符串
     * @return PublicKey
     */
    private static PublicKey getPublicKey(String publicKey) throws Exception {
        // 移除公钥中的头尾标记和换行符
        String publicKeyStr = publicKey.replace("-----BEGIN PUBLIC KEY-----", "")
                                       .replace("-----END PUBLIC KEY-----", "")
                                       .replace("\n", "")
                                       .replace("\r", "")
                                       .trim();
        
        byte[] keyBytes = base64Decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Base64 编码
     * 
     * @param bytes 待编码字节数组
     * @return 编码后的字符串
     */
    private static String base64Encode(byte[] bytes) {
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Base64 解码
     * 
     * @param str 待解码字符串
     * @return 解码后的字节数组
     */
    private static byte[] base64Decode(String str) {
        return java.util.Base64.getDecoder().decode(str);
    }

    /**
     * URL 编码
     * 
     * @param str 待编码字符串
     * @param charset 字符编码
     * @return 编码后的字符串
     */
    public static String urlEncode(String str, String charset) {
        try {
            if (charset == null || charset.isEmpty()) {
                return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
            } else {
                return URLEncoder.encode(str, charset);
            }
        } catch (UnsupportedEncodingException e) {
            log.error("URL编码异常", e);
            return str;
        }
    }

    /**
     * 生成支付宝签名
     * 
     * @param params 待签名参数
     * @param privateKey 私钥
     * @param charset 字符编码
     * @param signType 签名类型
     * @return 签名字符串
     */
    public static String generateSign(Map<String, String> params, String privateKey, 
                                     String charset, String signType) {
        try {
            // 获取待签名内容
            String signContent = getSignCheckContent(params);
            if (signContent == null || signContent.trim().isEmpty()) {
                log.error("待签名内容为空");
                return null;
            }

            log.debug("待签名内容: {}", signContent);

            // 生成签名
            return rsaSign(signContent, privateKey, charset, signType);

        } catch (Exception e) {
            log.error("生成支付宝签名异常", e);
            return null;
        }
    }
}
