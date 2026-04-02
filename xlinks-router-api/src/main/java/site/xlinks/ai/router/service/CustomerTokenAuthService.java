package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.entity.CustomerToken;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 客户 Token 认证服务
 * 负责验证客户身份和权限
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenAuthService {

    private final site.xlinks.ai.router.mapper.CustomerTokenMapper customerTokenMapper;

    /**
     * 验证客户 Token
     *
     * @param token 客户提供的 Bearer Token
     * @return 客户 Token 实体
     */
    public CustomerToken validateToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 不能为空");
        }

        // 查询数据库
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerToken::getTokenValue, token);
        CustomerToken customerToken = customerTokenMapper.selectOne(wrapper);

        if (customerToken == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Token");
        }

        // 检查状态
        if (customerToken.getStatus() == null || customerToken.getStatus() != 1) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 已禁用");
        }

        // 检查过期时间
        LocalDateTime expireTime = customerToken.getExpireTime();
        if (expireTime != null && LocalDateTime.now().isAfter(expireTime)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 已过期");
        }

        log.debug("Token validated for customer: {}", customerToken.getCustomerName());
        return customerToken;
    }

    /**
     * 检查客户是否有权访问指定模型
     *
     * @param customerToken 客户 Token
     * @param model         模型名称
     * @return 是否有权访问
     */
    public boolean hasPermissionForModel(CustomerToken customerToken, String model) {
        String allowedModels = customerToken.getAllowedModels();
        
        // 如果没有限制，则有权限
        if (allowedModels == null || allowedModels.isEmpty()) {
            return true;
        }

        try {
            // 解析 JSON 格式的允许模型列表
            List<String> allowedList = Arrays.asList(allowedModels.replaceAll("[\\[\\]\"]", "").split(","));
            return allowedList.contains(model);
        } catch (Exception e) {
            log.warn("Failed to parse allowed models, granting access", e);
            return true;
        }
    }
}
