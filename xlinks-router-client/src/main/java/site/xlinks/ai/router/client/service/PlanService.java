package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.PlanMapper;

import java.util.List;

/**
 * 订阅套餐服务
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanMapper planMapper;

    public List<Plan> listVisiblePlans(Long accountId) {
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Plan::getStatus, 1)
               .eq(Plan::getVisible, 1)
               .orderByAsc(Plan::getPrice);
        if (accountId != null) {
            List<Long> exceededPlanIds = planMapper.selectExceededPlanIdsByAccount(accountId);
            wrapper.notIn(exceededPlanIds != null && !exceededPlanIds.isEmpty(), Plan::getId, exceededPlanIds);
        }
        return planMapper.selectList(wrapper);
    }
}
