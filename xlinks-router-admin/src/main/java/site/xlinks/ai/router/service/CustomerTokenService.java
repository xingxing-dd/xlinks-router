package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Customer Token Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenService extends ServiceImpl<CustomerTokenMapper, CustomerToken> {

    public IPage<CustomerToken> pageQuery(Integer page, Integer pageSize, String customerName, Integer status) {
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(customerName), CustomerToken::getCustomerName, customerName)
               .eq(status != null, CustomerToken::getStatus, status)
               .orderByDesc(CustomerToken::getCreatedAt);
        
        return this.page(new Page<>(page, pageSize), wrapper);
    }

    public CustomerToken getById(Long id) {
        CustomerToken token = super.getById(id);
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Customer Token 不存在");
        }
        return token;
    }

    public CustomerToken getByTokenValue(String tokenValue) {
        String hash = hashToken(tokenValue);
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerToken::getTokenValue, hash);
        return this.getOne(wrapper);
    }

    /**
     * 创建 Token（生成并返回）
     */
    public CustomerToken create(CustomerToken token) {
        // 生成 Token
        String rawToken = "xlr_ct_" + UUID.randomUUID().toString().replace("-", "");
        String hashedToken = hashToken(rawToken);
        
        token.setTokenValue(hashedToken);
        super.save(token);
        
        // 返回原始 Token（只返回一次）
        token.setTokenValue(rawToken);
        return token;
    }

    public boolean update(CustomerToken token) {
        getById(token.getId());
        // 不更新 tokenValue
        token.setTokenValue(null);
        return super.updateById(token);
    }

    public boolean updateStatus(Long id, Integer status) {
        getById(id);
        CustomerToken token = new CustomerToken();
        token.setId(id);
        token.setStatus(status);
        return super.updateById(token);
    }

    /**
     * SHA256 哈希 Token
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
