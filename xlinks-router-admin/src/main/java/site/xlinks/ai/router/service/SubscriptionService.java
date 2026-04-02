package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.vo.SubscriptionRecordVO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService extends ServiceImpl<CustomerPlanMapper, CustomerPlan> {

    private final CustomerAccountMapper customerAccountMapper;

    public IPage<SubscriptionRecordVO> pageQuery(Integer page, Integer pageSize, String accountKeyword,
                                                 Long planId, Integer status, String source) {
        LambdaQueryWrapper<CustomerPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(planId != null, CustomerPlan::getPlanId, planId)
                .eq(status != null, CustomerPlan::getStatus, status)
                .eq(StringUtils.hasText(source), CustomerPlan::getSource, source == null ? null : source.trim())
                .orderByDesc(CustomerPlan::getCreatedAt);

        if (StringUtils.hasText(accountKeyword)) {
            List<Long> accountIds = findAccountIds(accountKeyword.trim());
            if (accountIds.isEmpty()) {
                Page<SubscriptionRecordVO> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            wrapper.in(CustomerPlan::getAccountId, accountIds);
        }

        Page<CustomerPlan> entityPage = this.page(new Page<>(page, pageSize), wrapper);
        List<SubscriptionRecordVO> records = enrich(entityPage.getRecords());
        Page<SubscriptionRecordVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public SubscriptionRecordVO getDetail(Long id) {
        CustomerPlan plan = getEntity(id);
        return enrich(Collections.singletonList(plan)).stream().findFirst().orElseThrow();
    }

    private List<SubscriptionRecordVO> enrich(List<CustomerPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> accountIds = plans.stream()
                .map(CustomerPlan::getAccountId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, CustomerAccount> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : customerAccountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        return plans.stream().map(plan -> {
            SubscriptionRecordVO vo = new SubscriptionRecordVO();
            BeanUtils.copyProperties(plan, vo);
            CustomerAccount account = accountMap.get(plan.getAccountId());
            if (account != null) {
                vo.setAccountName(resolveAccountDisplay(account));
                vo.setAccountPhone(account.getPhone());
                vo.setAccountEmail(account.getEmail());
                vo.setAccountStatus(account.getStatus());
            }
            vo.setDailyRemainingQuota(safeSubtract(plan.getDailyQuota(), plan.getUsedQuota()));
            vo.setTotalRemainingQuota(safeSubtract(plan.getTotalQuota(), plan.getTotalUsedQuota()));
            return vo;
        }).toList();
    }

    private CustomerPlan getEntity(Long id) {
        CustomerPlan plan = super.getById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Subscription record not found");
        }
        return plan;
    }

    private List<Long> findAccountIds(String keyword) {
        List<CustomerAccount> accounts = customerAccountMapper.selectList(new LambdaQueryWrapper<CustomerAccount>()
                .like(CustomerAccount::getUsername, keyword)
                .or()
                .like(CustomerAccount::getPhone, keyword)
                .or()
                .like(CustomerAccount::getEmail, keyword));
        return accounts.stream().map(CustomerAccount::getId).toList();
    }

    private String resolveAccountDisplay(CustomerAccount account) {
        if (StringUtils.hasText(account.getUsername())) {
            return account.getUsername();
        }
        if (StringUtils.hasText(account.getEmail())) {
            return account.getEmail();
        }
        if (StringUtils.hasText(account.getPhone())) {
            return account.getPhone();
        }
        return String.valueOf(account.getId());
    }

    private BigDecimal safeSubtract(BigDecimal left, BigDecimal right) {
        BigDecimal result = defaultDecimal(left).subtract(defaultDecimal(right));
        return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
