package site.xlinks.ai.router.merchant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import site.xlinks.ai.router.merchant.entity.AuthUser;

/**
 * 认证用户 Mapper。
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {
}