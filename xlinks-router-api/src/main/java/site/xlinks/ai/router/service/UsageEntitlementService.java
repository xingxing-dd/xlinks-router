package site.xlinks.ai.router.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.context.UsageDecision;
import site.xlinks.ai.router.entity.CustomerToken;

import java.util.ArrayList;
import java.util.List;

/**
 * 权益判定服务
 * 根据客户的套餐和余额状态，决定本次请求应该使用套餐还是余额
 * 
 * MVP 阶段：使用 Mock 实现，后续可扩展为真实的套餐/余额查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageEntitlementService {

    private final ObjectMapper objectMapper;

    /**
     * 判定当前请求的使用类型
     *
     * @param customerToken 客户 Token
     * @param requestModel 请求的模型名称
     * @return 权益判定结果
     */
    public UsageDecision decide(CustomerToken customerToken, String requestModel) {
        // MVP 阶段：默认返回无限权限
        // 后续可根据 customerToken 的 allowedModels 和套餐/余额状态进行更精细的判定
        
        log.debug("Deciding usage type for customer: {}, model: {}", 
                  customerToken.getCustomerName(), requestModel);

        // 解析套餐允许的模型列表
        List<String> packageAllowedModels = parseAllowedModels(customerToken.getAllowedModels());

        // MVP: 默认套餐和余额都启用
        // 后续可从真实的套餐/余额服务获取
        boolean packageEnabled = true;
        boolean balanceEnabled = true;

        // 计算当前使用类型
        int currentUsageType = calculateUsageType(packageEnabled, balanceEnabled, 
                                                   packageAllowedModels, requestModel);

        return UsageDecision.builder()
                .customerTokenId(customerToken.getId())
                .customerName(customerToken.getCustomerName())
                .packageEnabled(packageEnabled)
                .balanceEnabled(balanceEnabled)
                .currentUsageType(currentUsageType)
                .packageAllowedModels(packageAllowedModels)
                .unlimited(false) // MVP 阶段设为 false，后续可根据实际情况调整
                .build();
    }

    /**
     * 计算使用类型
     * 
     * 规则：
     * - 套餐和余额都启用(0): 优先套餐，如果请求模型不在套餐允许列表则切余额
     * - 仅套餐(1): 只能走套餐模式
     * - 仅余额(2): 只能走余额模式
     */
    private int calculateUsageType(boolean packageEnabled, boolean balanceEnabled,
                                    List<String> packageAllowedModels, String requestModel) {
        if (packageEnabled && balanceEnabled) {
            // 套餐优先：检查模型是否在套餐允许列表
            if (packageAllowedModels.isEmpty() || packageAllowedModels.contains(requestModel)) {
                return 0; // 套餐优先模式
            } else {
                return 2; // 模型不在套餐列表，切到余额模式
            }
        } else if (packageEnabled) {
            return 1; // 仅套餐
        } else if (balanceEnabled) {
            return 2; // 仅余额
        } else {
            // 都没有：返回 0（无限权限 mock）
            return 0;
        }
    }

    /**
     * 解析允许模型列表
     */
    private List<String> parseAllowedModels(String allowedModels) {
        if (allowedModels == null || allowedModels.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(allowedModels, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse allowed models: {}", allowedModels, e);
            return new ArrayList<>();
        }
    }
}
