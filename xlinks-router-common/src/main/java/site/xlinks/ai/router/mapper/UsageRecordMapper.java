package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.model.TokenUsageStats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Usage Record Mapper 接口
 */
@Mapper
public interface UsageRecordMapper extends BaseMapper<UsageRecord> {

    @Select("""
            SELECT COALESCE(SUM(total_cost), 0)
            FROM usage_records
            WHERE account_id = #{accountId}
              AND customer_token = #{customerToken}
              AND created_at >= #{startAt}
              AND created_at < #{endAt}
            """)
    BigDecimal sumTotalCostByDateRange(@Param("accountId") Long accountId,
                                       @Param("customerToken") String customerToken,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT customer_token AS customerToken,
                   COUNT(*) AS totalRequests,
                   MAX(created_at) AS lastUsedAt
            FROM usage_records
            WHERE account_id = #{accountId}
              AND customer_token IS NOT NULL
              AND customer_token != ''
            GROUP BY customer_token
            """)
    List<TokenUsageStats> aggregateTokenStatsByAccountId(@Param("accountId") Long accountId);
}
