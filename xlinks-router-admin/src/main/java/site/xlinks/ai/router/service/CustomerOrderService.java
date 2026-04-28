package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;
import site.xlinks.ai.router.vo.CustomerOrderVO;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final CustomerOrderMapper customerOrderMapper;
    private final CustomerAccountMapper customerAccountMapper;

    public IPage<CustomerOrderVO> pageQuery(Integer page, Integer pageSize,
                                            String orderKeyword,
                                            String accountKeyword,
                                            String orderType,
                                            String paymentChannel,
                                            Integer status,
                                            LocalDateTime startAt,
                                            LocalDateTime endAt) {
        List<Long> accountIds = resolveAccountIds(accountKeyword);
        if (accountIds != null && accountIds.isEmpty()) {
            return emptyPage(page, pageSize);
        }

        LambdaQueryWrapper<CustomerOrder> wrapper = new LambdaQueryWrapper<CustomerOrder>()
                .in(accountIds != null, CustomerOrder::getAccountId, accountIds)
                .eq(StringUtils.hasText(orderType), CustomerOrder::getOrderType, trim(orderType))
                .eq(StringUtils.hasText(paymentChannel), CustomerOrder::getPaymentChannel, trim(paymentChannel))
                .eq(status != null, CustomerOrder::getStatus, status)
                .ge(startAt != null, CustomerOrder::getCreatedAt, startAt)
                .le(endAt != null, CustomerOrder::getCreatedAt, endAt)
                .orderByDesc(CustomerOrder::getCreatedAt)
                .orderByDesc(CustomerOrder::getId);

        if (StringUtils.hasText(orderKeyword)) {
            String keyword = orderKeyword.trim();
            wrapper.and(q -> q.like(CustomerOrder::getOrderNo, keyword)
                    .or()
                    .like(CustomerOrder::getRefNo, keyword)
                    .or()
                    .like(CustomerOrder::getOrderTitle, keyword));
        }

        Page<CustomerOrder> entityPage = customerOrderMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<CustomerOrderVO> records = enrich(entityPage.getRecords());

        Page<CustomerOrderVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(records);
        return result;
    }

    private List<CustomerOrderVO> enrich(List<CustomerOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> accountIds = orders.stream()
                .map(CustomerOrder::getAccountId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());
        Map<Long, CustomerAccount> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : customerAccountMapper.selectBatchIds(accountIds).stream()
                .collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        return orders.stream().map(order -> {
            CustomerOrderVO vo = new CustomerOrderVO();
            BeanUtils.copyProperties(order, vo);
            CustomerAccount account = accountMap.get(order.getAccountId());
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
