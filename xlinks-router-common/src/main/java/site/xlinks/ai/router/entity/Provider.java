package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Provider 实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("providers")
public class Provider extends BaseEntity {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 提供商编码，唯一标识
     */
    private String providerCode;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 协议类型
     */
    /**
     * Supported request protocols, comma-separated. Empty means all protocols.
     */
    private String supportedProtocols;

    /**
     * Route priority. Higher value means higher priority.
     */
    private Integer priority;

    /**
     * Cache-hit usage extraction strategy.
     */
    private String cacheHitStrategy;

    /**
     * 基础请求 URL
     */
    private String baseUrl;

    /**
     * 服务商 Logo URL
     */
    private String providerLogo;

    /**
     * 服务商官网 URL
     */
    private String providerWebsite;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 备注
     */
    private String remark;
}
