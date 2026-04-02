package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.ActivationCodeGenerateDTO;
import site.xlinks.ai.router.dto.ActivationCodeUpdateDTO;
import site.xlinks.ai.router.entity.ActivationCodeStock;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.mapper.ActivationCodeStockMapper;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.vo.ActivationCodeGenerateVO;
import site.xlinks.ai.router.vo.ActivationCodeVO;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Activation code management service.
 */
@Service
@RequiredArgsConstructor
public class ActivationCodeStockService extends ServiceImpl<ActivationCodeStockMapper, ActivationCodeStock> {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_AVAILABLE = 1;
    private static final int STATUS_USED = 2;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PlanMapper planMapper;
    private final CustomerAccountMapper customerAccountMapper;

    public IPage<ActivationCodeVO> pageQuery(Integer page, Integer pageSize, Long planId, Integer status,
                                             String activationCode, String usedAccount, Long subscriptionId,
                                             String orderId) {
        LambdaQueryWrapper<ActivationCodeStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(planId != null, ActivationCodeStock::getPlanId, planId)
                .eq(status != null, ActivationCodeStock::getStatus, status)
                .eq(subscriptionId != null, ActivationCodeStock::getSubscriptionId, subscriptionId)
                .like(StringUtils.hasText(activationCode), ActivationCodeStock::getActivationCode, activationCode)
                .like(StringUtils.hasText(orderId), ActivationCodeStock::getOrderId, orderId)
                .orderByDesc(ActivationCodeStock::getCreatedAt);

        if (StringUtils.hasText(usedAccount)) {
            List<Long> accountIds = findAccountIds(usedAccount.trim());
            if (accountIds.isEmpty()) {
                Page<ActivationCodeVO> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            wrapper.in(ActivationCodeStock::getUsedBy, accountIds);
        }

        Page<ActivationCodeStock> entityPage = this.page(new Page<>(page, pageSize), wrapper);
        List<ActivationCodeVO> records = enrich(entityPage.getRecords());
        Page<ActivationCodeVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public ActivationCodeVO getDetail(Long id) {
        ActivationCodeStock stock = getEntity(id);
        return enrich(Collections.singletonList(stock)).stream().findFirst().orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivationCodeGenerateVO generate(ActivationCodeGenerateDTO dto) {
        Plan plan = getActivePlan(dto.getPlanId());
        Set<String> codes = new LinkedHashSet<>();
        while (codes.size() < dto.getQuantity()) {
            String code = buildCode(dto.getPrefix(), dto.getCodeLength());
            if (existsByCode(code) || codes.contains(code)) {
                continue;
            }
            codes.add(code);
        }

        for (String code : codes) {
            ActivationCodeStock stock = new ActivationCodeStock();
            stock.setActivationCode(code);
            stock.setPlanId(plan.getId());
            stock.setStatus(STATUS_AVAILABLE);
            stock.setRemark(dto.getRemark());
            super.save(stock);
        }

        ActivationCodeGenerateVO vo = new ActivationCodeGenerateVO();
        vo.setPlanId(plan.getId());
        vo.setPlanName(plan.getPlanName());
        vo.setGeneratedCount(codes.size());
        vo.setCodes(new ArrayList<>(codes));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public ActivationCodeVO update(Long id, ActivationCodeUpdateDTO dto) {
        ActivationCodeStock existing = getEntity(id);
        ActivationCodeStock stock = new ActivationCodeStock();
        stock.setId(id);

        if (dto.getPlanId() != null) {
            ensureEditable(existing);
            stock.setPlanId(getActivePlan(dto.getPlanId()).getId());
        }
        if (dto.getOrderId() != null) {
            ensureEditable(existing);
            stock.setOrderId(dto.getOrderId());
        }
        if (dto.getRemark() != null) {
            stock.setRemark(dto.getRemark());
        }

        super.updateById(stock);
        return getDetail(id);
    }

    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != STATUS_DISABLED && status != STATUS_AVAILABLE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Activation code status only supports 0 or 1");
        }
        ActivationCodeStock existing = getEntity(id);
        ensureEditable(existing);
        ActivationCodeStock stock = new ActivationCodeStock();
        stock.setId(id);
        stock.setStatus(status);
        super.updateById(stock);
    }

    public void deleteById(Long id) {
        ActivationCodeStock existing = getEntity(id);
        ensureEditable(existing);
        super.removeById(id);
    }

    private List<ActivationCodeVO> enrich(List<ActivationCodeStock> stocks) {
        if (stocks == null || stocks.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> planIds = stocks.stream().map(ActivationCodeStock::getPlanId).collect(Collectors.toSet());
        Set<Long> accountIds = stocks.stream()
                .map(ActivationCodeStock::getUsedBy)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, Plan> planMap = planIds.isEmpty()
                ? Collections.emptyMap()
                : planMapper.selectBatchIds(planIds).stream().collect(Collectors.toMap(Plan::getId, Function.identity()));
        Map<Long, CustomerAccount> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : customerAccountMapper.selectBatchIds(accountIds).stream().collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        return stocks.stream().map(stock -> {
            ActivationCodeVO vo = new ActivationCodeVO();
            BeanUtils.copyProperties(stock, vo);
            Plan plan = planMap.get(stock.getPlanId());
            if (plan != null) {
                vo.setPlanName(plan.getPlanName());
            }
            CustomerAccount account = accountMap.get(stock.getUsedBy());
            if (account != null) {
                vo.setUsedAccount(resolveAccountDisplay(account));
            }
            return vo;
        }).toList();
    }

    private ActivationCodeStock getEntity(Long id) {
        ActivationCodeStock stock = super.getById(id);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Activation code not found");
        }
        return stock;
    }

    private Plan getActivePlan(Long planId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Plan not found");
        }
        if (plan.getStatus() == null || plan.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Activation codes can only be generated for enabled plans");
        }
        return plan;
    }

    private void ensureEditable(ActivationCodeStock stock) {
        if (stock.getStatus() != null && stock.getStatus() == STATUS_USED) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Used activation code cannot be edited or deleted");
        }
    }

    private boolean existsByCode(String code) {
        return this.count(new LambdaQueryWrapper<ActivationCodeStock>()
                .eq(ActivationCodeStock::getActivationCode, code)) > 0;
    }

    private String buildCode(String prefix, Integer codeLength) {
        int length = codeLength == null ? 12 : codeLength;
        String normalizedPrefix = StringUtils.hasText(prefix) ? prefix.trim().toUpperCase() + "-" : "";
        StringBuilder builder = new StringBuilder(normalizedPrefix);
        for (int i = 0; i < length; i++) {
            builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return builder.toString();
    }

    private List<Long> findAccountIds(String keyword) {
        List<CustomerAccount> accounts = customerAccountMapper.selectList(new LambdaQueryWrapper<CustomerAccount>()
                .eq(CustomerAccount::getUsername, keyword)
                .or()
                .eq(CustomerAccount::getPhone, keyword)
                .or()
                .eq(CustomerAccount::getEmail, keyword));
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
}
