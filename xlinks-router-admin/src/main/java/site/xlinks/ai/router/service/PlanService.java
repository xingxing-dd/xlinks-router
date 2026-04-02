package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.dto.PlanCreateDTO;
import site.xlinks.ai.router.dto.PlanUpdateDTO;
import site.xlinks.ai.router.entity.ActivationCodeStock;
import site.xlinks.ai.router.entity.CustomerPlan;
import site.xlinks.ai.router.entity.Model;
import site.xlinks.ai.router.entity.Plan;
import site.xlinks.ai.router.entity.ThirdPartyPayLink;
import site.xlinks.ai.router.mapper.ActivationCodeStockMapper;
import site.xlinks.ai.router.mapper.CustomerPlanMapper;
import site.xlinks.ai.router.mapper.ModelMapper;
import site.xlinks.ai.router.mapper.PlanMapper;
import site.xlinks.ai.router.mapper.ThirdPartyPayLinkMapper;
import site.xlinks.ai.router.vo.PlanVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Plan management service.
 */
@Service
@RequiredArgsConstructor
public class PlanService extends ServiceImpl<PlanMapper, Plan> {

    private static final String TARGET_TYPE_PLAN = "plan";

    private final ThirdPartyPayLinkMapper thirdPartyPayLinkMapper;
    private final ActivationCodeStockMapper activationCodeStockMapper;
    private final CustomerPlanMapper customerPlanMapper;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    public IPage<PlanVO> pageQuery(Integer page, Integer pageSize, String planName, Integer status, Integer visible) {
        LambdaQueryWrapper<Plan> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(planName), Plan::getPlanName, planName)
                .eq(status != null, Plan::getStatus, status)
                .eq(visible != null, Plan::getVisible, visible)
                .orderByDesc(Plan::getCreatedAt);

        Page<Plan> entityPage = this.page(new Page<>(page, pageSize), wrapper);
        List<PlanVO> records = enrich(entityPage.getRecords());
        Page<PlanVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public PlanVO getDetail(Long id) {
        Plan plan = getEntity(id);
        Map<Long, ThirdPartyPayLink> payLinks = getPayLinkMap(Collections.singleton(plan.getId()));
        return toVO(plan, payLinks.get(plan.getId()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanVO create(PlanCreateDTO dto) {
        Plan plan = new Plan();
        plan.setPlanName(dto.getPlanName().trim());
        plan.setPrice(defaultDecimal(dto.getPrice()));
        plan.setDurationDays(dto.getDurationDays());
        plan.setDailyQuota(defaultDecimal(dto.getDailyQuota()));
        plan.setTotalQuota(defaultDecimal(dto.getTotalQuota()));
        plan.setAllowedModels(normalizeAllowedModels(dto.getAllowedModels()));
        plan.setStatus(normalizeBinaryStatus(dto.getStatus(), "Plan status"));
        plan.setVisible(normalizeBinaryStatus(dto.getVisible(), "Plan visibility"));
        plan.setRemark(dto.getRemark());
        validateQuota(plan.getDailyQuota(), plan.getTotalQuota());
        super.save(plan);
        syncPayLink(plan.getId(), dto.getPayUrl(), dto.getPayLinkStatus());
        return getDetail(plan.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public PlanVO update(Long id, PlanUpdateDTO dto) {
        Plan existing = getEntity(id);
        Plan plan = new Plan();
        plan.setId(id);
        if (StringUtils.hasText(dto.getPlanName())) {
            plan.setPlanName(dto.getPlanName().trim());
        }
        if (dto.getPrice() != null) {
            plan.setPrice(defaultDecimal(dto.getPrice()));
        }
        if (dto.getDurationDays() != null) {
            plan.setDurationDays(dto.getDurationDays());
        }
        if (dto.getDailyQuota() != null) {
            plan.setDailyQuota(defaultDecimal(dto.getDailyQuota()));
        }
        if (dto.getTotalQuota() != null) {
            plan.setTotalQuota(defaultDecimal(dto.getTotalQuota()));
        }
        if (dto.getAllowedModels() != null) {
            plan.setAllowedModels(normalizeAllowedModels(dto.getAllowedModels()));
        }
        if (dto.getStatus() != null) {
            plan.setStatus(normalizeBinaryStatus(dto.getStatus(), "Plan status"));
        }
        if (dto.getVisible() != null) {
            plan.setVisible(normalizeBinaryStatus(dto.getVisible(), "Plan visibility"));
        }
        if (dto.getRemark() != null) {
            plan.setRemark(dto.getRemark());
        }

        BigDecimal dailyQuota = plan.getDailyQuota() != null ? plan.getDailyQuota() : existing.getDailyQuota();
        BigDecimal totalQuota = plan.getTotalQuota() != null ? plan.getTotalQuota() : existing.getTotalQuota();
        validateQuota(dailyQuota, totalQuota);
        super.updateById(plan);

        if (dto.getPayUrl() != null || dto.getPayLinkStatus() != null) {
            updatePayLink(id, dto.getPayUrl(), dto.getPayLinkStatus());
        }
        return getDetail(id);
    }

    public void updateStatus(Long id, Integer status) {
        getEntity(id);
        Plan plan = new Plan();
        plan.setId(id);
        plan.setStatus(normalizeBinaryStatus(status, "Plan status"));
        super.updateById(plan);
    }

    public void updateVisible(Long id, Integer visible) {
        getEntity(id);
        Plan plan = new Plan();
        plan.setId(id);
        plan.setVisible(normalizeBinaryStatus(visible, "Plan visibility"));
        super.updateById(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        getEntity(id);
        long activationCodeCount = activationCodeStockMapper.selectCount(
                new LambdaQueryWrapper<ActivationCodeStock>().eq(ActivationCodeStock::getPlanId, id)
        );
        if (activationCodeCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Please remove activation codes under this plan before deleting it");
        }

        long customerPlanCount = customerPlanMapper.selectCount(
                new LambdaQueryWrapper<CustomerPlan>().eq(CustomerPlan::getPlanId, id)
        );
        if (customerPlanCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "This plan already has subscription records and cannot be deleted");
        }

        thirdPartyPayLinkMapper.delete(new LambdaQueryWrapper<ThirdPartyPayLink>()
                .eq(ThirdPartyPayLink::getTargetType, TARGET_TYPE_PLAN)
                .eq(ThirdPartyPayLink::getTargetId, id));
        super.removeById(id);
    }

    private List<PlanVO> enrich(List<Plan> plans) {
        if (plans == null || plans.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ThirdPartyPayLink> payLinks = getPayLinkMap(plans.stream().map(Plan::getId).collect(Collectors.toSet()));
        return plans.stream()
                .map(plan -> toVO(plan, payLinks.get(plan.getId())))
                .toList();
    }

    private Map<Long, ThirdPartyPayLink> getPayLinkMap(Set<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return thirdPartyPayLinkMapper.selectList(new LambdaQueryWrapper<ThirdPartyPayLink>()
                        .eq(ThirdPartyPayLink::getTargetType, TARGET_TYPE_PLAN)
                        .in(ThirdPartyPayLink::getTargetId, planIds)
                        .orderByDesc(ThirdPartyPayLink::getUpdatedAt))
                .stream()
                .collect(Collectors.toMap(ThirdPartyPayLink::getTargetId, Function.identity(), (left, right) -> left));
    }

    private PlanVO toVO(Plan plan, ThirdPartyPayLink payLink) {
        PlanVO vo = new PlanVO();
        BeanUtils.copyProperties(plan, vo);
        if (payLink != null) {
            vo.setPayUrl(payLink.getPayUrl());
            vo.setPayLinkStatus(payLink.getStatus());
        }
        return vo;
    }

    private Plan getEntity(Long id) {
        Plan plan = super.getById(id);
        if (plan == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Plan not found");
        }
        return plan;
    }

    private void validateQuota(BigDecimal dailyQuota, BigDecimal totalQuota) {
        if (dailyQuota == null || totalQuota == null) {
            return;
        }
        if (dailyQuota.compareTo(BigDecimal.ZERO) < 0 || totalQuota.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Quota must not be negative");
        }
        if (totalQuota.compareTo(dailyQuota) < 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Total quota must be greater than or equal to daily quota");
        }
    }

    private String normalizeAllowedModels(String allowedModels) {
        if (!StringUtils.hasText(allowedModels)) {
            return null;
        }
        try {
            List<String> models = objectMapper.readValue(allowedModels, new TypeReference<List<String>>() { });
            List<String> normalized = new ArrayList<>();
            for (String modelCode : models) {
                if (!StringUtils.hasText(modelCode)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "Allowed models contains blank model code");
                }
                normalized.add(modelCode.trim());
            }
            List<String> distinctModels = new ArrayList<>(new LinkedHashSet<>(normalized));
            validateModelCodes(distinctModels);
            return distinctModels.isEmpty() ? null : objectMapper.writeValueAsString(distinctModels);
        } catch (BusinessException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Allowed models must be a valid JSON string array");
        }
    }

    private void validateModelCodes(List<String> modelCodes) {
        if (modelCodes == null || modelCodes.isEmpty()) {
            return;
        }
        List<Model> models = modelMapper.selectList(new LambdaQueryWrapper<Model>().in(Model::getModelCode, modelCodes));
        Set<String> existingCodes = models.stream().map(Model::getModelCode).collect(Collectors.toSet());
        List<String> missing = modelCodes.stream().filter(code -> !existingCodes.contains(code)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "Unknown model codes: " + String.join(", ", missing));
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

    private void syncPayLink(Long planId, String payUrl, Integer status) {
        if (!StringUtils.hasText(payUrl)) {
            return;
        }
        ThirdPartyPayLink payLink = new ThirdPartyPayLink();
        payLink.setTargetId(planId);
        payLink.setTargetType(TARGET_TYPE_PLAN);
        payLink.setPayUrl(payUrl.trim());
        payLink.setStatus(normalizeBinaryStatus(status, "Pay link status"));
        thirdPartyPayLinkMapper.insert(payLink);
    }

    private void updatePayLink(Long planId, String payUrl, Integer payLinkStatus) {
        ThirdPartyPayLink existing = thirdPartyPayLinkMapper.selectOne(new LambdaQueryWrapper<ThirdPartyPayLink>()
                .eq(ThirdPartyPayLink::getTargetType, TARGET_TYPE_PLAN)
                .eq(ThirdPartyPayLink::getTargetId, planId)
                .last("limit 1"));

        if (payUrl != null && !StringUtils.hasText(payUrl)) {
            if (existing != null) {
                thirdPartyPayLinkMapper.deleteById(existing.getId());
            }
            return;
        }

        if (existing == null) {
            if (!StringUtils.hasText(payUrl)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "Pay link does not exist under this plan");
            }
            syncPayLink(planId, payUrl, payLinkStatus);
            return;
        }

        ThirdPartyPayLink payLink = new ThirdPartyPayLink();
        payLink.setId(existing.getId());
        if (StringUtils.hasText(payUrl)) {
            payLink.setPayUrl(payUrl.trim());
        }
        if (payLinkStatus != null) {
            payLink.setStatus(normalizeBinaryStatus(payLinkStatus, "Pay link status"));
        }
        thirdPartyPayLinkMapper.updateById(payLink);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
