package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Merchant-specific model to provider route override.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("merchant_provider_routes")
public class MerchantProviderRoute extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long accountId;

    private Long modelId;

    private Long providerId;
}
