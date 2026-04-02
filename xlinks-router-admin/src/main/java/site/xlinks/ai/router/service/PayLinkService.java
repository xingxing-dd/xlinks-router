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
import site.xlinks.ai.router.dto.PayLinkCreateDTO;
import site.xlinks.ai.router.dto.PayLinkUpdateDTO;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.ThirdPartyPayLink;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ThirdPartyPayLinkMapper;
import site.xlinks.ai.router.vo.PayLinkVO;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayLinkService extends ServiceImpl<ThirdPartyPayLinkMapper, ThirdPartyPayLink> {

    private static final String TARGET_TYPE_PLAN = "plan";

    private final PlanMapper planMapper;

    public IPage<PayLinkVO> pageQuery(Integer page, Integer pageSize, Long targetId, String planName, Integer status) {
        LambdaQueryWrapper<ThirdPartyPayLink> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ThirdPartyPayLink::getTargetType, TARGET_TYPE_PLAN)
                .eq(targetId != null, ThirdPartyPayLink::getTargetId, targetId)
                .eq(status != null, ThirdPartyPayLink::getStatus, status)
                .orderByDesc(ThirdPartyPayLink::getUpdatedAt);

        if (StringUtils.hasText(planName)) {
            List<Long> planIds = findPlanIds(planName.trim());
            if (planIds.isEmpty()) {
                Page<PayLinkVO> emptyPage = new Page<>(page, pageSize, 0);
                emptyPage.setRecords(Collections.emptyList());
                return emptyPage;
            }
            wrapper.in(ThirdPartyPayLink::getTargetId, planIds);
        }

        Page<ThirdPartyPayLink> entityPage = this.page(new Page<>(page, pageSize), wrapper);
        List<PayLinkVO> records = enrich(entityPage.getRecords());
        Page<PayLinkVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public PayLinkVO getDetail(Long id) {
        ThirdPartyPayLink payLink = getEntity(id);
        return enrich(Collections.singletonList(payLink)).stream().findFirst().orElseThrow();
    }

    @Transactional(rollbackFor = Exception.class)
    public PayLinkVO create(PayLinkCreateDTO dto) {
        Plan plan = getPlan(dto.getTargetId());
        ensureUniquePlanLink(plan.getId(), null);

        ThirdPartyPayLink payLink = new ThirdPartyPayLink();
        payLink.setTargetId(plan.getId());
        payLink.setTargetType(TARGET_TYPE_PLAN);
        payLink.setPayUrl(normalizePayUrl(dto.getPayUrl()));
        payLink.setStatus(normalizeBinaryStatus(dto.getStatus(), "Pay link status"));
        payLink.setRemark(dto.getRemark());
        super.save(payLink);
        return getDetail(payLink.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PayLinkVO update(Long id, PayLinkUpdateDTO dto) {
        ThirdPartyPayLink existing = getEntity(id);
        ThirdPartyPayLink payLink = new ThirdPartyPayLink();
        payLink.setId(id);

        if (dto.getPayUrl() != null) {
            payLink.setPayUrl(normalizePayUrl(dto.getPayUrl()));
        }
        if (dto.getStatus() != null) {
            payLink.setStatus(normalizeBinaryStatus(dto.getStatus(), "Pay link status"));
        }
        if (dto.getRemark() != null) {
            payLink.setRemark(dto.getRemark());
        }

        super.updateById(payLink);
        return getDetail(existing.getId());
    }

    public void updateStatus(Long id, Integer status) {
        getEntity(id);
        ThirdPartyPayLink payLink = new ThirdPartyPayLink();
        payLink.setId(id);
        payLink.setStatus(normalizeBinaryStatus(status, "Pay link status"));
        super.updateById(payLink);
    }

    public void deleteById(Long id) {
        getEntity(id);
        super.removeById(id);
    }

    private List<PayLinkVO> enrich(List<ThirdPartyPayLink> payLinks) {
        if (payLinks == null || payLinks.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> planIds = payLinks.stream()
                .map(ThirdPartyPayLink::getTargetId)
                .collect(Collectors.toSet());
        Map<Long, Plan> planMap = planIds.isEmpty()
                ? Collections.emptyMap()
                : planMapper.selectBatchIds(planIds).stream()
                .collect(Collectors.toMap(Plan::getId, Function.identity()));

        return payLinks.stream().map(payLink -> {
            PayLinkVO vo = new PayLinkVO();
            BeanUtils.copyProperties(payLink, vo);
            Plan plan = planMap.get(payLink.getTargetId());
            if (plan != null) {
                vo.setPlanName(plan.getPlanName());
                vo.setPlanStatus(plan.getStatus());
                vo.setPlanVisible(plan.getVisible());
            }
            return vo;
        }).toList();
    }

    private ThirdPartyPayLink getEntity(Long id) {
        ThirdPartyPayLink payLink = super.getById(id);
        if (payLink == null || !TARGET_TYPE_PLAN.equals(payLink.getTargetType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pay link not found");
        }
        return payLink;
    }

    private Plan getPlan(Long planId) {
        Plan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Plan not found");
        }
        return plan;
    }

    private List<Long> findPlanIds(String planName) {
        return planMapper.selectList(new LambdaQueryWrapper<Plan>()
                        .like(Plan::getPlanName, planName))
                .stream()
                .map(Plan::getId)
                .toList();
    }

    private void ensureUniquePlanLink(Long planId, Long currentId) {
        LambdaQueryWrapper<ThirdPartyPayLink> wrapper = new LambdaQueryWrapper<ThirdPartyPayLink>()
                .eq(ThirdPartyPayLink::getTargetType, TARGET_TYPE_PLAN)
                .eq(ThirdPartyPayLink::getTargetId, planId);
        if (currentId != null) {
            wrapper.ne(ThirdPartyPayLink::getId, currentId);
        }
        if (super.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "A pay link already exists under this plan");
        }
    }

    private Integer normalizeBinaryStatus(Integer value, String fieldName) {
        if (value == null) {
            return 1;
        }
        if (value != 0 && value != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " only supports 0 or 1");
        }
        return value;
    }

    private String normalizePayUrl(String payUrl) {
        if (!StringUtils.hasText(payUrl)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Pay URL must not be blank");
        }
        return payUrl.trim();
    }
}
