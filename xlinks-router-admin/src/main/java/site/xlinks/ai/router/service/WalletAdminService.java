package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerMainWalletFlow;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.entity.CustomerSubWallet;
import site.xlinks.ai.router.entity.CustomerSubWalletFlow;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerMainWalletFlowMapper;
import site.xlinks.ai.router.mapper.CustomerMainWalletMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;
import site.xlinks.ai.router.mapper.CustomerSubWalletFlowMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;
import site.xlinks.ai.router.vo.WalletDetailVO;
import site.xlinks.ai.router.vo.WalletBatchOpenResultVO;
import site.xlinks.ai.router.vo.WalletListVO;
import site.xlinks.ai.router.vo.WalletMainFlowVO;
import site.xlinks.ai.router.vo.WalletSubFlowVO;
import site.xlinks.ai.router.vo.WalletSubWalletVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Wallet admin service.
 */
@Service
@RequiredArgsConstructor
public class WalletAdminService {

    private final WalletService walletService;
    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerMainWalletMapper customerMainWalletMapper;
    private final CustomerMainWalletFlowMapper customerMainWalletFlowMapper;
    private final CustomerSubWalletFlowMapper customerSubWalletFlowMapper;
    private final CustomerOrderMapper customerOrderMapper;

    public IPage<WalletListVO> pageQuery(Integer page, Integer pageSize, String keyword, Integer walletStatus) {
        LambdaQueryWrapper<CustomerMainWallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(walletStatus != null, CustomerMainWallet::getStatus, walletStatus)
                .orderByDesc(CustomerMainWallet::getCreatedAt);
        if (StringUtils.hasText(keyword)) {
            List<Long> accountIds = customerAccountMapper.selectList(new LambdaQueryWrapper<CustomerAccount>()
                            .and(query -> query.like(CustomerAccount::getUsername, keyword.trim())
                                    .or()
                                    .like(CustomerAccount::getPhone, keyword.trim())
                                    .or()
                                    .like(CustomerAccount::getEmail, keyword.trim())))
                    .stream()
                    .map(CustomerAccount::getId)
                    .toList();
            if (accountIds.isEmpty()) {
                Page<WalletListVO> empty = new Page<>(page, pageSize, 0);
                empty.setRecords(Collections.emptyList());
                return empty;
            }
            wrapper.in(CustomerMainWallet::getAccountId, accountIds);
        }

        Page<CustomerMainWallet> entityPage = customerMainWalletMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Set<Long> accountIds = entityPage.getRecords().stream().map(CustomerMainWallet::getAccountId).collect(Collectors.toSet());
        Map<Long, CustomerAccount> accountMap = accountIds.isEmpty()
                ? Collections.emptyMap()
                : customerAccountMapper.selectBatchIds(accountIds).stream().collect(Collectors.toMap(CustomerAccount::getId, Function.identity()));

        Page<WalletListVO> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(entityPage.getRecords().stream().map(item -> toListVO(item, accountMap.get(item.getAccountId()))).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBatchOpenResultVO batchOpenMissingWallets() {
        List<CustomerAccount> accounts = customerAccountMapper.selectList(new LambdaQueryWrapper<CustomerAccount>()
                .select(CustomerAccount::getId));
        List<CustomerMainWallet> wallets = customerMainWalletMapper.selectList(new LambdaQueryWrapper<CustomerMainWallet>()
                .select(CustomerMainWallet::getAccountId));

        Set<Long> walletAccountIds = wallets.stream()
                .map(CustomerMainWallet::getAccountId)
                .collect(Collectors.toSet());

        List<Long> missingAccountIds = new ArrayList<>();
        for (CustomerAccount account : accounts) {
            if (account != null && account.getId() != null && !walletAccountIds.contains(account.getId())) {
                missingAccountIds.add(account.getId());
            }
        }

        for (Long accountId : missingAccountIds) {
            walletService.ensureWallet(accountId);
        }

        WalletBatchOpenResultVO vo = new WalletBatchOpenResultVO();
        vo.setTotalAccountCount(accounts.size());
        vo.setExistingWalletCount(walletAccountIds.size());
        vo.setCreatedWalletCount(missingAccountIds.size());
        return vo;
    }

    public WalletDetailVO getDetail(Long accountId) {
        CustomerAccount account = getAccount(accountId);
        WalletBundle bundle = walletService.ensureWallet(accountId);
        return toDetailVO(account, bundle);
    }

    public IPage<WalletMainFlowVO> pageMainFlows(Long accountId, Integer page, Integer pageSize) {
        CustomerMainWallet mainWallet = walletService.ensureWallet(accountId).getMainWallet();
        Page<CustomerMainWalletFlow> flowPage = customerMainWalletFlowMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<CustomerMainWalletFlow>()
                        .eq(CustomerMainWalletFlow::getMainWalletId, mainWallet.getId())
                        .orderByDesc(CustomerMainWalletFlow::getCreatedAt)
                        .orderByDesc(CustomerMainWalletFlow::getId));
        Page<WalletMainFlowVO> result = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        result.setRecords(flowPage.getRecords().stream().map(this::toMainFlowVO).toList());
        return result;
    }

    public IPage<WalletSubFlowVO> pageSubFlows(Long accountId, Integer page, Integer pageSize, String walletType) {
        CustomerMainWallet mainWallet = walletService.ensureWallet(accountId).getMainWallet();
        LambdaQueryWrapper<CustomerSubWalletFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerSubWalletFlow::getMainWalletId, mainWallet.getId())
                .eq(StringUtils.hasText(walletType), CustomerSubWalletFlow::getWalletType, walletType == null ? null : walletType.trim())
                .orderByDesc(CustomerSubWalletFlow::getCreatedAt)
                .orderByDesc(CustomerSubWalletFlow::getId);
        Page<CustomerSubWalletFlow> flowPage = customerSubWalletFlowMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<WalletSubFlowVO> result = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        result.setRecords(flowPage.getRecords().stream().map(this::toSubFlowVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO manualCredit(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.creditBasic(accountId, amount, WalletConstants.BIZ_TYPE_MANUAL_CREDIT, orderNo, normalizeRemark(remark, "manual credit"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO manualDebit(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.debitBasic(accountId, amount, WalletConstants.BIZ_TYPE_MANUAL_DEBIT, orderNo, normalizeRemark(remark, "manual debit"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO freeze(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.freezeFromBasic(accountId, amount, WalletConstants.BIZ_TYPE_FREEZE, orderNo, normalizeRemark(remark, "manual freeze"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO unfreeze(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.unfreezeToBasic(accountId, amount, WalletConstants.BIZ_TYPE_UNFREEZE, orderNo, normalizeRemark(remark, "manual unfreeze"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO creditPendingSettlement(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.creditPendingSettlement(accountId, amount, WalletConstants.BIZ_TYPE_PENDING_SETTLEMENT_IN,
                orderNo, normalizeRemark(remark, "pending settlement income"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO settlePending(Long accountId, BigDecimal amount, String orderNo, String remark) {
        walletService.settlePendingToBasic(accountId, amount, WalletConstants.BIZ_TYPE_SETTLEMENT,
                orderNo, normalizeRemark(remark, "settlement completed"));
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletDetailVO updateState(Long accountId, Integer allowIn, Integer allowOut, Integer status, String remark) {
        walletService.updateMainWalletState(accountId, allowIn, allowOut, status, remark);
        return getDetail(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approveWithdraw(String orderNo, String remark) {
        CustomerOrder order = getPendingWithdrawOrder(orderNo);
        walletService.deductFrozen(order.getAccountId(), order.getTotalAmount(), WalletConstants.BIZ_TYPE_WITHDRAW_SUCCESS,
                order.getOrderNo(), normalizeRemark(remark, "withdraw approved"));
        updateWithdrawOrderStatus(order, 1, normalizeRemark(remark, "withdraw approved"));
    }

    @Transactional(rollbackFor = Exception.class)
    public void rejectWithdraw(String orderNo, String remark) {
        CustomerOrder order = getPendingWithdrawOrder(orderNo);
        walletService.unfreezeToBasic(order.getAccountId(), order.getTotalAmount(), WalletConstants.BIZ_TYPE_WITHDRAW_CANCEL,
                order.getOrderNo(), normalizeRemark(remark, "withdraw rejected"));
        updateWithdrawOrderStatus(order, 2, normalizeRemark(remark, "withdraw rejected"));
    }

    private void updateWithdrawOrderStatus(CustomerOrder order, Integer status, String remark) {
        CustomerOrder update = new CustomerOrder();
        update.setStatus(status);
        update.setCompleteAt(LocalDateTime.now());
        update.setRemark(remark);
        int affected = customerOrderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getId, order.getId())
                .eq(CustomerOrder::getStatus, 0));
        if (affected <= 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "withdraw order state changed");
        }
    }

    private CustomerOrder getPendingWithdrawOrder(String orderNo) {
        CustomerOrder order = customerOrderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo)
                .eq(CustomerOrder::getOrderType, WalletConstants.ORDER_TYPE_WITHDRAW)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "withdraw order not found");
        }
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "withdraw order is not pending");
        }
        return order;
    }

    private CustomerAccount getAccount(Long accountId) {
        CustomerAccount account = customerAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return account;
    }

    private WalletListVO toListVO(CustomerMainWallet wallet, CustomerAccount account) {
        WalletListVO vo = new WalletListVO();
        vo.setAccountId(wallet.getAccountId());
        if (account != null) {
            vo.setUsername(account.getUsername());
            vo.setPhone(account.getPhone());
            vo.setEmail(account.getEmail());
        }
        vo.setWalletNo(wallet.getWalletNo());
        vo.setTotalBalance(wallet.getTotalBalance());
        vo.setAvailableBalance(wallet.getAvailableBalance());
        vo.setAllowIn(wallet.getAllowIn());
        vo.setAllowOut(wallet.getAllowOut());
        vo.setStatus(wallet.getStatus());
        vo.setCreatedAt(wallet.getCreatedAt());
        vo.setUpdatedAt(wallet.getUpdatedAt());
        return vo;
    }

    private WalletDetailVO toDetailVO(CustomerAccount account, WalletBundle bundle) {
        WalletDetailVO vo = new WalletDetailVO();
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setPhone(account.getPhone());
        vo.setEmail(account.getEmail());
        vo.setWalletNo(bundle.getMainWallet().getWalletNo());
        vo.setTotalBalance(bundle.getMainWallet().getTotalBalance());
        vo.setAvailableBalance(bundle.getMainWallet().getAvailableBalance());
        vo.setAllowIn(bundle.getMainWallet().getAllowIn());
        vo.setAllowOut(bundle.getMainWallet().getAllowOut());
        vo.setStatus(bundle.getMainWallet().getStatus());
        vo.setRemark(bundle.getMainWallet().getRemark());
        vo.setCreatedAt(bundle.getMainWallet().getCreatedAt());
        vo.setUpdatedAt(bundle.getMainWallet().getUpdatedAt());
        vo.setSubWallets(bundle.getSubWallets().stream().map(this::toSubWalletVO).toList());
        return vo;
    }

    private WalletSubWalletVO toSubWalletVO(CustomerSubWallet wallet) {
        WalletSubWalletVO vo = new WalletSubWalletVO();
        vo.setWalletNo(wallet.getWalletNo());
        vo.setWalletType(wallet.getWalletType());
        vo.setBalance(wallet.getBalance());
        vo.setStatus(wallet.getStatus());
        vo.setRemark(wallet.getRemark());
        return vo;
    }

    private WalletMainFlowVO toMainFlowVO(CustomerMainWalletFlow flow) {
        WalletMainFlowVO vo = new WalletMainFlowVO();
        vo.setId(flow.getId());
        vo.setOrderNo(flow.getOrderNo());
        vo.setBizType(flow.getBizType());
        vo.setDirection(flow.getDirection());
        vo.setChangeAmount(flow.getChangeAmount());
        vo.setTotalBalanceBefore(flow.getTotalBalanceBefore());
        vo.setTotalBalanceAfter(flow.getTotalBalanceAfter());
        vo.setAvailableBalanceBefore(flow.getAvailableBalanceBefore());
        vo.setAvailableBalanceAfter(flow.getAvailableBalanceAfter());
        vo.setRemark(flow.getRemark());
        vo.setCreatedAt(flow.getCreatedAt());
        return vo;
    }

    private WalletSubFlowVO toSubFlowVO(CustomerSubWalletFlow flow) {
        WalletSubFlowVO vo = new WalletSubFlowVO();
        vo.setId(flow.getId());
        vo.setOrderNo(flow.getOrderNo());
        vo.setWalletType(flow.getWalletType());
        vo.setBizType(flow.getBizType());
        vo.setDirection(flow.getDirection());
        vo.setChangeAmount(flow.getChangeAmount());
        vo.setBalanceBefore(flow.getBalanceBefore());
        vo.setBalanceAfter(flow.getBalanceAfter());
        vo.setRemark(flow.getRemark());
        vo.setCreatedAt(flow.getCreatedAt());
        return vo;
    }

    private String normalizeRemark(String remark, String defaultRemark) {
        return StringUtils.hasText(remark) ? remark.trim() : defaultRemark;
    }
}
