package site.xlinks.ai.router.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import site.xlinks.ai.router.entity.CustomerPlan;

/**
 * CustomerPlan Mapper 接口
 */
@Mapper
public interface CustomerPlanMapper extends BaseMapper<CustomerPlan> {

    @Select("SELECT * FROM customer_plans WHERE id = #{id} FOR UPDATE")
    CustomerPlan selectByIdForUpdate(@Param("id") Long id);
}
