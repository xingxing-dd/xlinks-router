package site.xlinks.ai.router.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Admin account entity.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin_accounts")
public class AdminAccount extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;

    private String displayName;

    private String email;

    private String phone;

    private String password;

    private Integer status;

    private LocalDateTime lastLoginAt;

    @TableLogic
    private Integer deleted;

    private String remark;
}
