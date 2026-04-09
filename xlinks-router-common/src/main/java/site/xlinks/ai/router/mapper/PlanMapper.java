package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.Plan;

import java.util.List;

/**
 * Plan mapper.
 */
@Mapper
public interface PlanMapper extends BaseMapper<Plan> {

    @Select("""
            SELECT p.id
            FROM plans p
            JOIN customer_plans cp ON cp.plan_id = p.id
            WHERE cp.account_id = #{accountId}
              AND p.max_purchase_count IS NOT NULL
              AND (
                cp.source LIKE 'order:%'
                OR cp.source = 'purchase'
              )
            GROUP BY p.id, p.max_purchase_count
            HAVING COUNT(1) >= p.max_purchase_count
            """)
    List<Long> selectExceededPlanIdsByAccount(@Param("accountId") Long accountId);

    @Select("""
            SELECT COUNT(1)
            FROM customer_plans
            WHERE account_id = #{accountId}
              AND plan_id = #{planId}
              AND (
                source LIKE 'order:%'
                OR source = 'purchase'
              )
            """)
    long selectPurchaseCountByAccountAndPlan(@Param("accountId") Long accountId,
                                             @Param("planId") Long planId);
}