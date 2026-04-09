package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.client.dto.token.CreateCustomerTokenRequest;
import site.xlinks.ai.router.client.dto.token.UpdateCustomerTokenRequest;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerToken;
import site.xlinks.ai.router.mapper.CustomerTokenMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 客户端 Customer Token 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerTokenService {

    private final CustomerTokenMapper customerTokenMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IPage<CustomerToken> pageTokens(Long accountId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerToken::getAccountId, accountId)
               .orderByDesc(CustomerToken::getCreatedAt);
        return customerTokenMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    public CustomerToken createToken(Long accountId, String customerName, CreateCustomerTokenRequest request, String operator) {
        CustomerToken token = new CustomerToken();
        token.setAccountId(accountId);
        token.setCustomerName(customerName);
        token.setTokenName(request.getTokenName());
        token.setStatus(1);
        token.setTokenValue(generateTokenValue());
        token.setAllowedModels(toJson(request.getAllowedModels()));
        token.setExpireTime(resolveExpireTime(request.getExpireDays()));
        token.setCreateBy(operator);
        token.setUpdateBy(operator);

        customerTokenMapper.insert(token);
        return token;
    }

    public void updateToken(Long accountId, Long id, UpdateCustomerTokenRequest request, String operator) {
        CustomerToken existing = getByIdAndAccount(id, accountId);

        CustomerToken update = new CustomerToken();
        update.setId(existing.getId());
        update.setTokenName(request.getTokenName());
        update.setStatus(request.getStatus());
        update.setAllowedModels(toJson(request.getAllowedModels()));
        update.setExpireTime(parseExpireTime(request.getExpireTime()));
        update.setUpdateBy(operator);

        customerTokenMapper.updateById(update);
    }

    public void updateStatus(Long accountId, Long id, Integer status, String operator) {
        CustomerToken existing = getByIdAndAccount(id, accountId);
        CustomerToken update = new CustomerToken();
        update.setId(existing.getId());
        update.setStatus(status);
        update.setUpdateBy(operator);
        customerTokenMapper.updateById(update);
    }

    public void deleteToken(Long accountId, Long id) {
        CustomerToken existing = getByIdAndAccount(id, accountId);
        customerTokenMapper.deleteById(existing.getId());
    }

    public CustomerToken refreshToken(Long accountId, Long id, String operator) {
        CustomerToken existing = getByIdAndAccount(id, accountId);
        CustomerToken update = new CustomerToken();
        update.setId(existing.getId());
        update.setTokenValue(generateTokenValue());
        update.setUpdateBy(operator);
        customerTokenMapper.updateById(update);
        existing.setTokenValue(update.getTokenValue());
        return existing;
    }

    private CustomerToken getByIdAndAccount(Long id, Long accountId) {
        LambdaQueryWrapper<CustomerToken> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerToken::getId, id)
               .eq(CustomerToken::getAccountId, accountId);
        CustomerToken token = customerTokenMapper.selectOne(wrapper);
        if (token == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Token不存在或无权限访问");
        }
        return token;
    }

    private String generateTokenValue() {
        return "sk-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "allowedModels格式错误");
        }
    }

    private LocalDateTime resolveExpireTime(Integer expireDays) {
        if (expireDays == null || expireDays <= 0) {
            return null;
        }
        return LocalDateTime.now().plusDays(expireDays);
    }

    private LocalDateTime parseExpireTime(String expireTime) {
        if (expireTime == null || expireTime.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(expireTime.replace(" ", "T"));
    }
}
