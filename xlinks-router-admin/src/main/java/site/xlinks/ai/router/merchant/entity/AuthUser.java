package site.xlinks.ai.router.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import site.xlinks.ai.router.entity.BaseEntity;

import java.time.LocalDateTime;

/**
 * 认证用户实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_users")
public class AuthUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String mobile;

    private Integer status;

    private LocalDateTime lastLoginAt;

    private String lastLoginIp;

    private String remark;
}