package site.xlinks.ai.router.merchant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体。
 */
@Data
@TableName("auth_login_logs")
public class AuthLoginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long userId;

    private String email;

    private String loginType;

    private Integer loginStatus;

    private String loginIp;

    private String userAgent;

    private String failureReason;

    @TableField("created_at")
    private LocalDateTime createdAt;
}