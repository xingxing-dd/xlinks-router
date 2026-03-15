package site.xlinks.ai.router.context;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 权益判定结果
 * 用于判断当前请求应该使用套餐还是余额
 */
@Data
@Builder
public class UsageDecision {

    /**
     * 客户 Token ID
     */
    private Long customerTokenId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 套餐是否启用
     */
    private boolean packageEnabled;

    /**
     * 余额是否启用
     */
    private boolean balanceEnabled;

    /**
     * 当前使用类型：0-不限制，1-仅套餐，2-仅余额
     * 根据套餐和余额的状态综合计算
     */
    private Integer currentUsageType;

    /**
     * 套餐允许的模型列表（JSON格式）
     */
    private List<String> packageAllowedModels;

    /**
     * 是否无限权限（mock模式）
     */
    private boolean unlimited;
}
