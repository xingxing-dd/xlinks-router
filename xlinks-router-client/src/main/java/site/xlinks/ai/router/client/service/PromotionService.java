package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.client.config.PromotionProperties;
import site.xlinks.ai.router.client.dto.promotion.PromotionInfoResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRecordItemResponse;
import site.xlinks.ai.router.client.dto.promotion.PromotionRuleResponse;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.PromotionRecord;
import site.xlinks.ai.router.entity.PromotionRule;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.PromotionRecordMapper;
import site.xlinks.ai.router.mapper.PromotionRuleMapper;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推广模块服务。
 */
@Service
@RequiredArgsConstructor
public class PromotionService {

    private static final String INVITE_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_CODE_LENGTH = 8;
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INVALID = 2;
    private static final int REWARD_TYPE_REGISTER = 1;
    private static final int REWARD_TYPE_FIRST_RECHARGE = 2;
    private static final int REWARD_TYPE_REBATE = 3;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final CustomerAccountMapper customerAccountMapper;
    private final PromotionRecordMapper promotionRecordMapper;
    private final PromotionRuleMapper promotionRuleMapper;
    private final PromotionProperties promotionProperties;

    public PromotionInfoResponse getPromotionInfo(Long accountId) {
        CustomerAccount account = getAccountOrThrow(accountId);
        ensureInviteCode(account);

        int totalReferrals = Math.toIntExact(countInvitees(accountId, null));
        int activeReferrals = Math.toIntExact(countInvitees(accountId, STATUS_ACTIVE));

        PromotionInfoResponse response = new PromotionInfoResponse();
        response.setReferralCode(account.getInviteCode());
        response.setReferralLink(buildReferralLink(account.getInviteCode()));
        response.setTotalReferrals(totalReferrals);
        response.setActiveReferrals(activeReferrals);
        response.setTotalEarnings(sumRewards(accountId, null));
        response.setPendingEarnings(sumRewards(accountId, STATUS_PENDING));
        return response;
    }

    public IPage<PromotionRecordItemResponse> pageRecords(Long accountId, Integer page, Integer pageSize) {
        LambdaQueryWrapper<PromotionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRecord::getInviterUserId, accountId)
                .orderByDesc(PromotionRecord::getCreatedAt);

        Page<PromotionRecord> recordPage = promotionRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Set<Long> inviteeIds = recordPage.getRecords().stream()
                .map(PromotionRecord::getInviteeUserId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, CustomerAccount> inviteeMap = inviteeIds.isEmpty()
                ? Map.of()
                : customerAccountMapper.selectBatchIds(inviteeIds).stream()
                .collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        List<PromotionRecordItemResponse> records = recordPage.getRecords().stream()
                .map(item -> toRecordResponse(item, inviteeMap.get(item.getInviteeUserId())))
                .toList();

        Page<PromotionRecordItemResponse> result = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        result.setRecords(records);
        return result;
    }

    public PromotionRuleResponse getPromotionRules() {
        List<PromotionRule> rules = listActiveRules();
        if (rules.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "推广规则未配置");
        }

        PromotionRuleResponse response = new PromotionRuleResponse();
        response.setRules(rules.stream().map(this::toRuleItem).toList());

        rules.forEach(rule -> {
            if (REWARD_TYPE_REGISTER == (rule.getRewardType() == null ? -1 : rule.getRewardType())) {
                response.setRegisterReward(defaultDecimal(rule.getRewardAmount()));
            } else if (REWARD_TYPE_FIRST_RECHARGE == (rule.getRewardType() == null ? -1 : rule.getRewardType())) {
                response.setFirstRechargeRate(defaultDecimal(rule.getRewardRate()));
            } else if (REWARD_TYPE_REBATE == (rule.getRewardType() == null ? -1 : rule.getRewardType())) {
                response.setConsumptionRate(defaultDecimal(rule.getRewardRate()));
            }

            if (rule.getSettlementDay() != null) {
                response.setSettlementDay(rule.getSettlementDay());
                response.setSettlementDescription(rule.getDescription());
            }
        });

        return response;
    }

    public boolean validateInviteCode(String inviteCode) {
        return StringUtils.hasText(inviteCode) && getByInviteCode(inviteCode.trim()) != null;
    }

    public void bindInviterAndInitReward(CustomerAccount account, String inviteCode) {
        ensureInviteCode(account);
        if (!StringUtils.hasText(inviteCode)) {
            return;
        }

        CustomerAccount inviter = getByInviteCode(inviteCode.trim());
        if (inviter == null) {
            throw new BusinessException(ErrorCode.INVITE_CODE_INVALID);
        }
        if (inviter.getId().equals(account.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能填写自己的邀请码");
        }
        if (account.getInvitedBy() != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该账户已绑定邀请人");
        }

        account.setInvitedBy(inviter.getId());
        customerAccountMapper.updateById(account);

        createRegisterReward(inviter, account);
    }

    public void createRegisterReward(CustomerAccount inviter, CustomerAccount invitee) {
        PromotionRule registerRule = getRequiredRuleByRewardType(REWARD_TYPE_REGISTER);
        PromotionRecord record = new PromotionRecord();
        record.setInviterUserId(inviter.getId());
        record.setInviteeUserId(invitee.getId());
        record.setInviteCode(inviter.getInviteCode());
        record.setRewardType(REWARD_TYPE_REGISTER);
        record.setRewardAmount(defaultDecimal(registerRule.getRewardAmount()));
        record.setRewardRate(BigDecimal.ZERO);
        record.setStatus(STATUS_ACTIVE);
        record.setRemark(registerRule.getDescription());
        promotionRecordMapper.insert(record);
    }

    public void createFirstRechargeReward(Long inviteeAccountId, BigDecimal rechargeAmount, String orderNo) {
        if (rechargeAmount == null || rechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CustomerAccount invitee = getAccountOrThrow(inviteeAccountId);
        if (invitee.getInvitedBy() == null) {
            return;
        }
        if (existsRewardRecord(inviteeAccountId, REWARD_TYPE_FIRST_RECHARGE, orderNo)) {
            return;
        }
        CustomerAccount inviter = getAccountOrThrow(invitee.getInvitedBy());
        PromotionRule firstRechargeRule = getRequiredRuleByRewardType(REWARD_TYPE_FIRST_RECHARGE);
        BigDecimal rate = percentageToRatio(firstRechargeRule.getRewardRate());
        PromotionRecord record = buildRewardRecord(inviter, invitee, REWARD_TYPE_FIRST_RECHARGE,
                rechargeAmount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP), rate.multiply(new BigDecimal("100")), orderNo,
                firstRechargeRule.getDescription());
        promotionRecordMapper.insert(record);
    }

    public void createConsumptionReward(Long inviteeAccountId, BigDecimal consumptionAmount, String orderNo) {
        if (consumptionAmount == null || consumptionAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        CustomerAccount invitee = getAccountOrThrow(inviteeAccountId);
        if (invitee.getInvitedBy() == null) {
            return;
        }
        CustomerAccount inviter = getAccountOrThrow(invitee.getInvitedBy());
        PromotionRule rebateRule = getRequiredRuleByRewardType(REWARD_TYPE_REBATE);
        BigDecimal rate = percentageToRatio(rebateRule.getRewardRate());
        PromotionRecord record = buildRewardRecord(inviter, invitee, REWARD_TYPE_REBATE,
                consumptionAmount.multiply(rate).setScale(2, BigDecimal.ROUND_HALF_UP), rate.multiply(new BigDecimal("100")), orderNo,
                rebateRule.getDescription());
        promotionRecordMapper.insert(record);
    }

    public void markRewardInvalid(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return;
        }
        LambdaQueryWrapper<PromotionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRecord::getSourceOrderNo, orderNo);
        promotionRecordMapper.selectList(wrapper).forEach(record -> {
            record.setStatus(STATUS_INVALID);
            promotionRecordMapper.updateById(record);
        });
    }

    public void ensureInviteCode(CustomerAccount account) {
        if (StringUtils.hasText(account.getInviteCode())) {
            return;
        }
        account.setInviteCode(generateUniqueInviteCode());
        customerAccountMapper.updateById(account);
    }

    private PromotionRecordItemResponse toRecordResponse(PromotionRecord record, CustomerAccount invitee) {
        return new PromotionRecordItemResponse(
                String.valueOf(record.getId()),
                resolveDisplayName(invitee),
                maskEmail(invitee == null ? null : invitee.getEmail()),
                (invitee != null && invitee.getCreatedAt() != null ? invitee.getCreatedAt() : record.getCreatedAt()).format(DATE_FORMATTER),
                mapStatus(record.getStatus()),
                record.getRewardAmount() == null ? BigDecimal.ZERO : record.getRewardAmount()
        );
    }

    private CustomerAccount getAccountOrThrow(Long accountId) {
        CustomerAccount account = customerAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return account;
    }

    private CustomerAccount getByInviteCode(String inviteCode) {
        LambdaQueryWrapper<CustomerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerAccount::getInviteCode, inviteCode.toUpperCase());
        return customerAccountMapper.selectOne(wrapper);
    }

    private boolean existsRewardRecord(Long inviteeAccountId, Integer rewardType, String orderNo) {
        LambdaQueryWrapper<PromotionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRecord::getInviteeUserId, inviteeAccountId)
                .eq(PromotionRecord::getRewardType, rewardType);
        if (StringUtils.hasText(orderNo)) {
            wrapper.eq(PromotionRecord::getSourceOrderNo, orderNo);
        }
        return promotionRecordMapper.selectCount(wrapper) > 0;
    }

    private long countInvitees(Long accountId, Integer status) {
        LambdaQueryWrapper<CustomerAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerAccount::getInvitedBy, accountId);
        if (status != null) {
            wrapper.eq(CustomerAccount::getStatus, status);
        }
        return customerAccountMapper.selectCount(wrapper);
    }

    private BigDecimal sumRewards(Long accountId, Integer status) {
        LambdaQueryWrapper<PromotionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRecord::getInviterUserId, accountId);
        if (status != null) {
            wrapper.eq(PromotionRecord::getStatus, status);
        }
        return promotionRecordMapper.selectList(wrapper).stream()
                .map(PromotionRecord::getRewardAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildReferralLink(String inviteCode) {
        return promotionProperties.getReferralLinkPrefix() + inviteCode;
    }

    private List<PromotionRule> listActiveRules() {
        LambdaQueryWrapper<PromotionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRule::getStatus, STATUS_ACTIVE)
                .orderByAsc(PromotionRule::getSortOrder)
                .orderByAsc(PromotionRule::getId);
        return promotionRuleMapper.selectList(wrapper);
    }

    private PromotionRule getRequiredRuleByRewardType(Integer rewardType) {
        LambdaQueryWrapper<PromotionRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromotionRule::getRewardType, rewardType)
                .eq(PromotionRule::getStatus, STATUS_ACTIVE)
                .last("limit 1");
        PromotionRule rule = promotionRuleMapper.selectOne(wrapper);
        if (rule == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "推广规则未配置: " + rewardType);
        }
        return rule;
    }

    private PromotionRuleResponse.RuleItem toRuleItem(PromotionRule rule) {
        PromotionRuleResponse.RuleItem item = new PromotionRuleResponse.RuleItem();
        item.setRuleCode(rule.getRuleCode());
        item.setRuleName(rule.getRuleName());
        item.setRewardType(rule.getRewardType());
        item.setRewardAmount(rule.getRewardAmount());
        item.setRewardRate(rule.getRewardRate());
        item.setSettlementDay(rule.getSettlementDay());
        item.setDescription(rule.getDescription());
        item.setSortOrder(rule.getSortOrder());
        item.setIconType(rule.getIconType());
        return item;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal percentageToRatio(BigDecimal percentage) {
        return defaultDecimal(percentage).divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
    }

    private String resolveDisplayName(CustomerAccount account) {
        if (account == null) {
            return "-";
        }
        if (StringUtils.hasText(account.getUsername())) {
            return account.getUsername();
        }
        if (StringUtils.hasText(account.getEmail())) {
            String emailPrefix = account.getEmail().split("@", 2)[0];
            return emailPrefix.length() <= 2 ? emailPrefix + "**" : emailPrefix.substring(0, 2) + "**";
        }
        return "用户" + account.getId();
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "-";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        if (local.length() <= 2) {
            return local.substring(0, 1) + "***@" + parts[1];
        }
        return local.substring(0, 2) + "***@" + parts[1];
    }

    private String mapStatus(Integer status) {
        return STATUS_ACTIVE == (status == null ? STATUS_PENDING : status) ? "active" : "pending";
    }

    private PromotionRecord buildRewardRecord(CustomerAccount inviter,
                                              CustomerAccount invitee,
                                              Integer rewardType,
                                              BigDecimal rewardAmount,
                                              BigDecimal rewardRate,
                                              String orderNo,
                                              String remark) {
        PromotionRecord record = new PromotionRecord();
        record.setInviterUserId(inviter.getId());
        record.setInviteeUserId(invitee.getId());
        record.setInviteCode(inviter.getInviteCode());
        record.setRewardType(rewardType);
        record.setRewardAmount(rewardAmount);
        record.setRewardRate(rewardRate);
        record.setStatus(STATUS_PENDING);
        record.setSourceOrderNo(orderNo);
        record.setRemark(remark);
        return record;
    }

    private String generateUniqueInviteCode() {
        for (int i = 0; i < 10; i++) {
            String candidate = randomInviteCode();
            LambdaQueryWrapper<CustomerAccount> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CustomerAccount::getInviteCode, candidate);
            if (customerAccountMapper.selectCount(wrapper) == 0) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邀请码生成失败，请稍后再试");
    }

    private String randomInviteCode() {
        StringBuilder builder = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            int index = SECURE_RANDOM.nextInt(INVITE_CODE_CHARS.length());
            builder.append(INVITE_CODE_CHARS.charAt(index));
        }
        return builder.toString();
    }
}