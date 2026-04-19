package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import site.xlinks.ai.router.entity.CustomerToken;

import java.math.BigDecimal;

/**
 * Customer Token Mapper 接口
 */
@Mapper
public interface CustomerTokenMapper extends BaseMapper<CustomerToken> {

    @Update("""
            <script>
            UPDATE customer_tokens
            SET used_quota = GREATEST(COALESCE(used_quota, 0), COALESCE(#{usedQuota}, 0)),
                total_used_quota = COALESCE(total_used_quota, 0) + #{amount},
                updated_at = NOW()
            WHERE id = #{tokenId}
            </script>
            """)
    int syncQuotaUsage(@Param("tokenId") Long tokenId,
                       @Param("usedQuota") BigDecimal usedQuota,
                       @Param("amount") BigDecimal amount);

    @Update("""
            UPDATE customer_tokens
            SET used_quota = 0,
                updated_at = NOW()
            WHERE used_quota IS NOT NULL
              AND used_quota <> 0
            """)
    int resetDailyQuotaAtMidnight();
}
