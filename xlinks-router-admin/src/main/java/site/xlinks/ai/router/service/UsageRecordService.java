package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.UsageRecord;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.UsageRecordAdminMapper;
import site.xlinks.ai.router.vo.UsageRecordAccountSummaryVO;
import site.xlinks.ai.router.vo.UsageRecordFlowVO;
import site.xlinks.ai.router.vo.UsageRecordModelSummaryVO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsageRecordService {

    private final UsageRecordAdminMapper usageRecordAdminMapper;
    private final CustomerAccountMapper customerAccountMapper;

    public IPage<UsageRecordFlowVO> pageFlowQuery(Integer page, Integer pageSize, String accountKeyword,
                                                  String modelCode, String providerCode, String usageType,
                                                  String requestId, Integer responseStatus,
                                                  LocalDateTime startAt, LocalDateTime endAt) {
        List<Long> accountIds = resolveAccountIds(accountKeyword);
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        long total = usageRecordAdminMapper.countFlow(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                trim(requestId),
                responseStatus,
                startAt,
                endAt
        );
        long offset = Math.max((long) (page - 1) * pageSize, 0L);
        List<UsageRecord> entityRecords = total == 0
                ? Collections.emptyList()
                : usageRecordAdminMapper.selectFlow(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                trim(requestId),
                responseStatus,
                startAt,
                endAt,
                offset,
                pageSize
        );
        List<UsageRecordFlowVO> records = enrich(entityRecords);
        Page<UsageRecordFlowVO> result = new Page<>(page, pageSize, total);
        result.setRecords(records);
        return result;
    }

    public IPage<UsageRecordAccountSummaryVO> pageAccountSummary(Integer page, Integer pageSize, String accountKeyword,
                                                                 String modelCode, String providerCode, String usageType,
                                                                 LocalDateTime startAt, LocalDateTime endAt) {
        List<Long> accountIds = resolveAccountIds(accountKeyword);
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        long total = usageRecordAdminMapper.countAccountSummary(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                startAt,
                endAt
        );
        long offset = Math.max((long) (page - 1) * pageSize, 0L);
        List<UsageRecordAccountSummaryVO> records = total == 0
                ? Collections.emptyList()
                : usageRecordAdminMapper.selectAccountSummary(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                startAt,
                endAt,
                offset,
                pageSize
        );

        Page<UsageRecordAccountSummaryVO> result = new Page<>(page, pageSize, total);
        result.setRecords(records);
        return result;
    }

    public IPage<UsageRecordModelSummaryVO> pageModelSummary(Integer page, Integer pageSize, String accountKeyword,
                                                             String modelCode, String providerCode, String usageType,
                                                             LocalDateTime startAt, LocalDateTime endAt) {
        List<Long> accountIds = resolveAccountIds(accountKeyword);
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        long total = usageRecordAdminMapper.countModelSummary(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                startAt,
                endAt
        );
        long offset = Math.max((long) (page - 1) * pageSize, 0L);
        List<UsageRecordModelSummaryVO> records = total == 0
                ? Collections.emptyList()
                : usageRecordAdminMapper.selectModelSummary(
                accountIds,
                trim(modelCode),
                trim(providerCode),
                trim(usageType),
                startAt,
                endAt,
                offset,
                pageSize
        );

        Page<UsageRecordModelSummaryVO> result = new Page<>(page, pageSize, total);
        result.setRecords(records);
        return result;
    }

    private List<UsageRecordFlowVO> enrich(List<UsageRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> accountIds = records.stream()
                .map(UsageRecord::getAccountId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, CustomerAccount> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : customerAccountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        return records.stream().map(record -> {
            UsageRecordFlowVO vo = new UsageRecordFlowVO();
            BeanUtils.copyProperties(record, vo);
            CustomerAccount account = accountMap.get(record.getAccountId());
            if (account != null) {
                vo.setAccountName(resolveAccountDisplay(account));
                vo.setAccountPhone(account.getPhone());
                vo.setAccountEmail(account.getEmail());
            }
            return vo;
        }).toList();
    }

    private List<Long> resolveAccountIds(String accountKeyword) {
        if (!StringUtils.hasText(accountKeyword)) {
            return null;
        }
        String keyword = accountKeyword.trim();
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

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private <T> IPage<T> emptyPage(Integer page, Integer pageSize) {
        Page<T> result = new Page<>(page, pageSize, 0);
        result.setRecords(Collections.emptyList());
        return result;
    }
}
