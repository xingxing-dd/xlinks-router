package site.xlinks.ai.router.merchant.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.merchant.config.AuthProperties;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA 加解密服务。
 */
@Service
@RequiredArgsConstructor
public class RsaCryptoService {

    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    private final AuthProperties authProperties;

    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @PostConstruct
    public void init() {
        try {
            if (StringUtils.hasText(authProperties.getRsaPublicKey())
                    && StringUtils.hasText(authProperties.getRsaPrivateKey())) {
                this.publicKey = (RSAPublicKey) parsePublicKey(authProperties.getRsaPublicKey());
                this.privateKey = (RSAPrivateKey) parsePrivateKey(authProperties.getRsaPrivateKey());
                return;
            }
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(authProperties.getRsaKeySize());
            KeyPair keyPair = generator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("初始化 RSA 密钥失败", ex);
        }
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public String decrypt(String encryptedText) {
        if (!StringUtils.hasText(encryptedText)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "加密密码不能为空");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "密码解密失败，请确认使用最新 RSA 公钥");
        }
    }

    private PublicKey parsePublicKey(String base64PublicKey) throws GeneralSecurityException {
        byte[] decoded = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }

    private PrivateKey parsePrivateKey(String base64PrivateKey) throws GeneralSecurityException {
        byte[] decoded = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }
}