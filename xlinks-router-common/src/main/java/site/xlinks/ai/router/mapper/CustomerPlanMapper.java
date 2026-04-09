package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import site.xlinks.ai.router.entity.CustomerPlan;

import java.time.LocalDateTime;

/**
 * CustomerPlan Mapper 接口
 */
@Mapper
public interface CustomerPlanMapper extends BaseMapper<CustomerPlan> {

    @Select("SELECT * FROM customer_plans WHERE id = #{id} FOR UPDATE")
    CustomerPlan selectByIdForUpdate(@Param("id") Long id);

    @Select("""
            SELECT *
            FROM customer_plans
            WHERE account_id = #{accountId}
              AND status = 1
              AND daily_quota IS NOT NULL
              AND daily_quota > 0
              AND (
                used_quota < daily_quota
                OR quota_refresh_time IS NULL
                OR DATE(quota_refresh_time) <> #{today}
              )
            ORDER BY plan_expire_time ASC
            LIMIT 1
            """)
    CustomerPlan selectFirstAvailablePlan(@Param("accountId") Long accountId,
                                          @Param("today") java.time.LocalDate today);

    @Update("""
            UPDATE customer_plans
            SET used_quota = 0,
                quota_refresh_time = #{refreshTime},
                updated_at = NOW()
            WHERE status = 1
              AND daily_quota IS NOT NULL
              AND used_quota = daily_quota
            """)
    int resetDailyQuotaAtMidnight(@Param("refreshTime") LocalDateTime refreshTime);
}
