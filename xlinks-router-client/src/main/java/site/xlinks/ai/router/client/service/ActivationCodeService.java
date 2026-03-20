package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.xlinks.ai.router.client.context.CustomerAccountContext;
import site.xlinks.ai.router.client.dto.plan.ActivationCodeConsumeResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.ActivationCodeStock;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.ActivationCodeStockMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.PlanMapper;

import java.time.LocalDateTime;

/**
 * 激活码兑换服务
 */
@Service
@RequiredArgsConstructor
public class ActivationCodeService {

    private static final int STATUS_AVAILABLE = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_USED = 2;

    private final ActivationCodeStockMapper activationCodeStockMapper;
    private final PlanMapper planMapper;
    private final CustomerPlanMapper customerPlanMapper;

    @Transactional(rollbackFor = Exception.class)
    public ActivationCodeConsumeResponse consume(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不能为空");
        }

        CustomerAccount account = CustomerAccountContext.getAccount();
        if (account == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }

        String normalizedCode = code.trim();

        LambdaQueryWrapper<ActivationCodeStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ActivationCodeStock::getActivationCode, normalizedCode);
        ActivationCodeStock stock = activationCodeStockMapper.selectOne(wrapper);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不存在或不可用");
        }

        if (stock.getStatus() != null && stock.getStatus() == STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "激活码不可用");
        }

        if (stock.getStatus() != null && stock.getStatus() == STATUS_USED) {
            if (stock.getUsedBy() != null && stock.getUsedBy().equals(account.getId())) {
                return buildResponseFromExisting(stock);
            }
            throw new BusinessException(ErrorCode.FORBIDDEN, "激活码已被使用");
        }

        Plan plan = planMapper.selectById(stock.getPlanId());
        if (plan == null || plan.getStatus() == null || plan.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "套餐不存在或已下架");
        }

        CustomerPlan customerPlan = new CustomerPlan();
        customerPlan.setAccountId(account.getId());
        customerPlan.setPlanId(plan.getId());
        customerPlan.setPlanName(plan.getPlanName());
        customerPlan.setPrice(plan.getPrice());
        customerPlan.setDurationDays(plan.getDurationDays());
        customerPlan.setDailyQuota(plan.getDailyQuota());
        customerPlan.setTotalQuota(plan.getTotalQuota());
        customerPlan.setUsedQuota(0);
        customerPlan.setTotalUsedQuota(0);
        customerPlan.setQuotaRefreshTime(LocalDateTime.now());
        customerPlan.setPlanExpireTime(LocalDateTime.now().plusDays(plan.getDurationDays()));
        customerPlan.setStatus(1);
        customerPlan.setSource("activation_code");
        customerPlan.setCreateBy(String.valueOf(account.getId()));
        customerPlan.setUpdateBy(String.valueOf(account.getId()));
        customerPlanMapper.insert(customerPlan);

        stock.setStatus(STATUS_USED);
        stock.setUsedAt(LocalDateTime.now());
        stock.setUsedBy(account.getId());
        stock.setSubscriptionId(customerPlan.getId());
        stock.setUpdateBy(String.valueOf(account.getId()));
        activationCodeStockMapper.updateById(stock);

        ActivationCodeConsumeResponse response = new ActivationCodeConsumeResponse();
        response.setMessage("激活成功");
        response.setActivatedPlanId(String.valueOf(plan.getId()));
        response.setActivatedPlanName(plan.getPlanName());
        response.setExpireTime(customerPlan.getPlanExpireTime().toString().replace('T', ' '));
        response.setSubscriptionId(String.valueOf(customerPlan.getId()));
        return response;
    }

    private ActivationCodeConsumeResponse buildResponseFromExisting(ActivationCodeStock stock) {
        ActivationCodeConsumeResponse response = new ActivationCodeConsumeResponse();
        Plan plan = planMapper.selectById(stock.getPlanId());
        response.setMessage("激活成功");
        if (plan != null) {
            response.setActivatedPlanId(String.valueOf(plan.getId()));
            response.setActivatedPlanName(plan.getPlanName());
        }
        response.setExpireTime(stock.getUsedAt() != null ? stock.getUsedAt().toString().replace('T', ' ') : null);
        response.setSubscriptionId(stock.getSubscriptionId() != null ? String.valueOf(stock.getSubscriptionId()) : null);
        return response;
    }
}
