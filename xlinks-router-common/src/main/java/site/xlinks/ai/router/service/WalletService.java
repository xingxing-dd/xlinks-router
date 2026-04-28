package site.xlinks.ai.router.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerMainWalletFlow;
import site.xlinks.ai.router.entity.CustomerSubWallet;
import site.xlinks.ai.router.entity.CustomerSubWalletFlow;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerMainWalletFlowMapper;
import site.xlinks.ai.router.mapper.CustomerMainWalletMapper;
import site.xlinks.ai.router.mapper.CustomerSubWalletFlowMapper;
import site.xlinks.ai.router.mapper.CustomerSubWalletMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Wallet and ledger domain service.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final BigDecimal ZERO = new BigDecimal("0.000000");
    private static final int MONEY_SCALE = 6;
    private static final DateTimeFormatter WALLET_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final CustomerAccountMapper customerAccountMapper;
    private final CustomerMainWalletMapper customerMainWalletMapper;
    private final CustomerSubWalletMapper customerSubWalletMapper;
    private final CustomerMainWalletFlowMapper customerMainWalletFlowMapper;
    private final CustomerSubWalletFlowMapper customerSubWalletFlowMapper;

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle ensureWallet(Long accountId) {
        CustomerAccount account = requireAccount(accountId);
        CustomerMainWallet mainWallet = customerMainWalletMapper.selectOne(
                new LambdaQueryWrapper<CustomerMainWallet>()
                        .eq(CustomerMainWallet::getAccountId, accountId)
                        .last("limit 1")
        );
        if (mainWallet == null) {
            tryCreateWallet(account);
        }
        WalletBundle bundle = loadWallet(accountId);
        ensureSubWalletTypes(bundle.getMainWallet(), bundle.getSubWallets());
        return loadWallet(accountId);
    }

    public WalletBundle getWallet(Long accountId) {
        return ensureWallet(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle updateMainWalletState(Long accountId, Integer allowIn, Integer allowOut, Integer status, String remark) {
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        if (allowIn != null) {
            validateBinaryFlag(allowIn, "allowIn");
            mainWallet.setAllowIn(allowIn);
        }
        if (allowOut != null) {
            validateBinaryFlag(allowOut, "allowOut");
            mainWallet.setAllowOut(allowOut);
        }
        if (status != null) {
            validateBinaryFlag(status, "wallet status");
            mainWallet.setStatus(status);
        }
        if (remark != null) {
            mainWallet.setRemark(remark);
        }
        customerMainWalletMapper.updateById(mainWallet);
        return loadWallet(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle creditBasic(Long accountId,
                                    BigDecimal amount,
                                    String bizType,
                                    String orderNo,
                                    String remark) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        assertAllowIn(mainWallet);
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            return loadWallet(accountId);
        }

        CustomerSubWallet basicWallet = requireSubWallet(bundle, WalletConstants.SUB_WALLET_BASIC);
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());
        BigDecimal basicBefore = defaultMoney(basicWallet.getBalance());

        mainWallet.setTotalBalance(totalBefore.add(normalizedAmount));
        mainWallet.setAvailableBalance(availableBefore.add(normalizedAmount));
        basicWallet.setBalance(basicBefore.add(normalizedAmount));

        customerMainWalletMapper.updateById(mainWallet);
        customerSubWalletMapper.updateById(basicWallet);

        insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_IN, normalizedAmount,
                totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
        insertSubFlow(mainWallet, basicWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_IN, normalizedAmount,
                basicBefore, basicWallet.getBalance(), remark);
        return loadWallet(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle debitBasic(Long accountId,
                                   BigDecimal amount,
                                   String bizType,
                                   String orderNo,
                                   String remark) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        assertAllowOut(mainWallet);
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            return loadWallet(accountId);
        }

        CustomerSubWallet basicWallet = requireSubWallet(bundle, WalletConstants.SUB_WALLET_BASIC);
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());
        BigDecimal basicBefore = defaultMoney(basicWallet.getBalance());
        assertSufficientBalance(basicBefore, normalizedAmount, "basic wallet balance is insufficient");

        mainWallet.setTotalBalance(totalBefore.subtract(normalizedAmount));
        mainWallet.setAvailableBalance(availableBefore.subtract(normalizedAmount));
        basicWallet.setBalance(basicBefore.subtract(normalizedAmount));

        customerMainWalletMapper.updateById(mainWallet);
        customerSubWalletMapper.updateById(basicWallet);

        insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, normalizedAmount,
                totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
        insertSubFlow(mainWallet, basicWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, normalizedAmount,
                basicBefore, basicWallet.getBalance(), remark);
        return loadWallet(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public BasicWalletDebitResult debitBasicAllowOverdraftToZero(Long accountId,
                                                                 BigDecimal amount,
                                                                 String bizType,
                                                                 String orderNo,
                                                                 String remark) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        assertAllowOut(mainWallet);
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            WalletBundle latestBundle = loadWallet(accountId);
            return new BasicWalletDebitResult(
                    latestBundle,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
            );
        }

        CustomerSubWallet basicWallet = requireSubWallet(bundle, WalletConstants.SUB_WALLET_BASIC);
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());
        BigDecimal basicBefore = defaultMoney(basicWallet.getBalance());
        BigDecimal actualDebit = basicBefore.min(normalizedAmount);
        BigDecimal shortfallAmount = normalizedAmount.subtract(actualDebit).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        if (actualDebit.compareTo(BigDecimal.ZERO) > 0) {
            mainWallet.setTotalBalance(totalBefore.subtract(actualDebit));
            mainWallet.setAvailableBalance(availableBefore.subtract(actualDebit));
            basicWallet.setBalance(basicBefore.subtract(actualDebit));

            customerMainWalletMapper.updateById(mainWallet);
            customerSubWalletMapper.updateById(basicWallet);

            insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, actualDebit,
                    totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
            insertSubFlow(mainWallet, basicWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, actualDebit,
                    basicBefore, basicWallet.getBalance(), remark);
        }

        return new BasicWalletDebitResult(loadWallet(accountId), actualDebit, shortfallAmount);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle freezeFromBasic(Long accountId,
                                        BigDecimal amount,
                                        String bizType,
                                        String orderNo,
                                        String remark) {
        return transferBetweenSubWallets(accountId, amount, bizType, orderNo, remark,
                WalletConstants.SUB_WALLET_BASIC, WalletConstants.SUB_WALLET_FROZEN,
                ZERO, normalizeMoney(amount, "amount").negate());
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle unfreezeToBasic(Long accountId,
                                        BigDecimal amount,
                                        String bizType,
                                        String orderNo,
                                        String remark) {
        return transferBetweenSubWallets(accountId, amount, bizType, orderNo, remark,
                WalletConstants.SUB_WALLET_FROZEN, WalletConstants.SUB_WALLET_BASIC,
                ZERO, normalizeMoney(amount, "amount"));
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle creditPendingSettlement(Long accountId,
                                                BigDecimal amount,
                                                String bizType,
                                                String orderNo,
                                                String remark) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        assertAllowIn(mainWallet);
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            return loadWallet(accountId);
        }

        CustomerSubWallet pendingWallet = requireSubWallet(bundle, WalletConstants.SUB_WALLET_PENDING_SETTLEMENT);
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());
        BigDecimal pendingBefore = defaultMoney(pendingWallet.getBalance());

        mainWallet.setTotalBalance(totalBefore.add(normalizedAmount));
        pendingWallet.setBalance(pendingBefore.add(normalizedAmount));

        customerMainWalletMapper.updateById(mainWallet);
        customerSubWalletMapper.updateById(pendingWallet);

        insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_IN, normalizedAmount,
                totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
        insertSubFlow(mainWallet, pendingWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_IN, normalizedAmount,
                pendingBefore, pendingWallet.getBalance(), remark);
        return loadWallet(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle settlePendingToBasic(Long accountId,
                                             BigDecimal amount,
                                             String bizType,
                                             String orderNo,
                                             String remark) {
        return transferBetweenSubWallets(accountId, amount, bizType, orderNo, remark,
                WalletConstants.SUB_WALLET_PENDING_SETTLEMENT, WalletConstants.SUB_WALLET_BASIC,
                ZERO, normalizeMoney(amount, "amount"));
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletBundle deductFrozen(Long accountId,
                                     BigDecimal amount,
                                     String bizType,
                                     String orderNo,
                                     String remark) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            return loadWallet(accountId);
        }

        CustomerSubWallet frozenWallet = requireSubWallet(bundle, WalletConstants.SUB_WALLET_FROZEN);
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());
        BigDecimal frozenBefore = defaultMoney(frozenWallet.getBalance());
        assertSufficientBalance(frozenBefore, normalizedAmount, "frozen wallet balance is insufficient");

        mainWallet.setTotalBalance(totalBefore.subtract(normalizedAmount));
        frozenWallet.setBalance(frozenBefore.subtract(normalizedAmount));

        customerMainWalletMapper.updateById(mainWallet);
        customerSubWalletMapper.updateById(frozenWallet);

        insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, normalizedAmount,
                totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
        insertSubFlow(mainWallet, frozenWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, normalizedAmount,
                frozenBefore, frozenWallet.getBalance(), remark);
        return loadWallet(accountId);
    }

    private WalletBundle transferBetweenSubWallets(Long accountId,
                                                   BigDecimal amount,
                                                   String bizType,
                                                   String orderNo,
                                                   String remark,
                                                   String fromWalletType,
                                                   String toWalletType,
                                                   BigDecimal totalDelta,
                                                   BigDecimal availableDelta) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "amount");
        WalletBundle bundle = lockWallet(accountId);
        CustomerMainWallet mainWallet = bundle.getMainWallet();
        if (hasProcessed(mainWallet.getId(), orderNo, bizType)) {
            return loadWallet(accountId);
        }

        CustomerSubWallet fromWallet = requireSubWallet(bundle, fromWalletType);
        CustomerSubWallet toWallet = requireSubWallet(bundle, toWalletType);
        BigDecimal fromBefore = defaultMoney(fromWallet.getBalance());
        BigDecimal toBefore = defaultMoney(toWallet.getBalance());
        BigDecimal totalBefore = defaultMoney(mainWallet.getTotalBalance());
        BigDecimal availableBefore = defaultMoney(mainWallet.getAvailableBalance());

        assertSufficientBalance(fromBefore, normalizedAmount, fromWalletType + " wallet balance is insufficient");

        fromWallet.setBalance(fromBefore.subtract(normalizedAmount));
        toWallet.setBalance(toBefore.add(normalizedAmount));
        mainWallet.setTotalBalance(totalBefore.add(defaultMoney(totalDelta)));
        mainWallet.setAvailableBalance(availableBefore.add(defaultMoney(availableDelta)));

        customerSubWalletMapper.updateById(fromWallet);
        customerSubWalletMapper.updateById(toWallet);
        customerMainWalletMapper.updateById(mainWallet);

        insertMainFlow(mainWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_TRANSFER, normalizedAmount,
                totalBefore, mainWallet.getTotalBalance(), availableBefore, mainWallet.getAvailableBalance(), remark);
        insertSubFlow(mainWallet, fromWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_OUT, normalizedAmount,
                fromBefore, fromWallet.getBalance(), remark);
        insertSubFlow(mainWallet, toWallet, orderNo, bizType, WalletConstants.FLOW_DIRECTION_IN, normalizedAmount,
                toBefore, toWallet.getBalance(), remark);
        return loadWallet(accountId);
    }

    private void tryCreateWallet(CustomerAccount account) {
        CustomerMainWallet mainWallet = new CustomerMainWallet();
        mainWallet.setAccountId(account.getId());
        mainWallet.setWalletNo(generateWalletNo("MW", account.getId()));
        mainWallet.setTotalBalance(ZERO);
        mainWallet.setAvailableBalance(ZERO);
        mainWallet.setAllowIn(1);
        mainWallet.setAllowOut(1);
        mainWallet.setStatus(1);
        mainWallet.setDeleted(0);
        mainWallet.setRemark("Wallet auto-created");
        try {
            customerMainWalletMapper.insert(mainWallet);
            createSubWallet(mainWallet.getId(), WalletConstants.SUB_WALLET_BASIC, account.getId(), "Basic wallet");
            createSubWallet(mainWallet.getId(), WalletConstants.SUB_WALLET_FROZEN, account.getId(), "Frozen wallet");
            createSubWallet(mainWallet.getId(), WalletConstants.SUB_WALLET_PENDING_SETTLEMENT, account.getId(), "Pending settlement wallet");
        } catch (DuplicateKeyException ignored) {
            // Wallet already created by another transaction.
        }
    }

    private void ensureSubWalletTypes(CustomerMainWallet mainWallet, List<CustomerSubWallet> subWallets) {
        Map<String, CustomerSubWallet> walletMap = toWalletMap(subWallets);
        createMissingSubWallet(mainWallet, walletMap, WalletConstants.SUB_WALLET_BASIC, "Basic wallet");
        createMissingSubWallet(mainWallet, walletMap, WalletConstants.SUB_WALLET_FROZEN, "Frozen wallet");
        createMissingSubWallet(mainWallet, walletMap, WalletConstants.SUB_WALLET_PENDING_SETTLEMENT, "Pending settlement wallet");
    }

    private void createMissingSubWallet(CustomerMainWallet mainWallet,
                                        Map<String, CustomerSubWallet> walletMap,
                                        String walletType,
                                        String remark) {
        if (walletMap.containsKey(walletType)) {
            return;
        }
        try {
            createSubWallet(mainWallet.getId(), walletType, mainWallet.getAccountId(), remark);
        } catch (DuplicateKeyException ignored) {
            // Ignore concurrent creation.
        }
    }

    private void createSubWallet(Long mainWalletId, String walletType, Long accountId, String remark) {
        CustomerSubWallet subWallet = new CustomerSubWallet();
        subWallet.setMainWalletId(mainWalletId);
        subWallet.setWalletNo(generateWalletNo("SW", accountId));
        subWallet.setWalletType(walletType);
        subWallet.setBalance(ZERO);
        subWallet.setStatus(1);
        subWallet.setDeleted(0);
        subWallet.setRemark(remark);
        customerSubWalletMapper.insert(subWallet);
    }

    private WalletBundle loadWallet(Long accountId) {
        CustomerMainWallet mainWallet = customerMainWalletMapper.selectOne(
                new LambdaQueryWrapper<CustomerMainWallet>()
                        .eq(CustomerMainWallet::getAccountId, accountId)
                        .last("limit 1")
        );
        if (mainWallet == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "wallet not found");
        }
        List<CustomerSubWallet> subWallets = customerSubWalletMapper.selectList(
                new LambdaQueryWrapper<CustomerSubWallet>()
                        .eq(CustomerSubWallet::getMainWalletId, mainWallet.getId())
                        .orderByAsc(CustomerSubWallet::getId)
        );
        subWallets.sort(Comparator.comparing(CustomerSubWallet::getWalletType));
        return new WalletBundle(mainWallet, subWallets);
    }

    private WalletBundle lockWallet(Long accountId) {
        ensureWallet(accountId);
        CustomerMainWallet mainWallet = customerMainWalletMapper.selectByAccountIdForUpdate(accountId);
        if (mainWallet == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "wallet not found");
        }
        List<CustomerSubWallet> subWallets = customerSubWalletMapper.selectByMainWalletIdForUpdate(mainWallet.getId());
        return new WalletBundle(mainWallet, sortSubWallets(subWallets));
    }

    private List<CustomerSubWallet> sortSubWallets(List<CustomerSubWallet> subWallets) {
        List<CustomerSubWallet> sorted = new ArrayList<>(subWallets == null ? List.of() : subWallets);
        sorted.sort(Comparator.comparing(CustomerSubWallet::getWalletType));
        return sorted;
    }

    private CustomerSubWallet requireSubWallet(WalletBundle bundle, String walletType) {
        return bundle.getSubWallets().stream()
                .filter(item -> Objects.equals(walletType, item.getWalletType()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "sub wallet not found: " + walletType));
    }

    private Map<String, CustomerSubWallet> toWalletMap(List<CustomerSubWallet> subWallets) {
        Map<String, CustomerSubWallet> map = new HashMap<>();
        if (subWallets == null) {
            return map;
        }
        for (CustomerSubWallet subWallet : subWallets) {
            if (subWallet != null && StringUtils.hasText(subWallet.getWalletType())) {
                map.putIfAbsent(subWallet.getWalletType(), subWallet);
            }
        }
        return map;
    }

    private CustomerAccount requireAccount(Long accountId) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "accountId is required");
        }
        CustomerAccount account = customerAccountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return account;
    }

    private void assertAllowIn(CustomerMainWallet mainWallet) {
        if (mainWallet.getStatus() == null || mainWallet.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "wallet is disabled");
        }
        if (mainWallet.getAllowIn() == null || mainWallet.getAllowIn() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "wallet incoming is disabled");
        }
    }

    private void assertAllowOut(CustomerMainWallet mainWallet) {
        if (mainWallet.getStatus() == null || mainWallet.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "wallet is disabled");
        }
        if (mainWallet.getAllowOut() == null || mainWallet.getAllowOut() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "wallet outgoing is disabled");
        }
    }

    private void assertSufficientBalance(BigDecimal balance, BigDecimal amount, String message) {
        if (defaultMoney(balance).compareTo(defaultMoney(amount)) < 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, message);
        }
    }

    private void validateBinaryFlag(Integer value, String fieldName) {
        if (value == null || (value != 0 && value != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " only supports 0 or 1");
        }
    }

    private boolean hasProcessed(Long mainWalletId, String orderNo, String bizType) {
        if (mainWalletId == null || !StringUtils.hasText(orderNo) || !StringUtils.hasText(bizType)) {
            return false;
        }
        return customerMainWalletFlowMapper.selectByOrderNoAndBizType(mainWalletId, orderNo.trim(), bizType.trim()) != null;
    }

    private void insertMainFlow(CustomerMainWallet mainWallet,
                                String orderNo,
                                String bizType,
                                String direction,
                                BigDecimal changeAmount,
                                BigDecimal totalBefore,
                                BigDecimal totalAfter,
                                BigDecimal availableBefore,
                                BigDecimal availableAfter,
                                String remark) {
        CustomerMainWalletFlow flow = new CustomerMainWalletFlow();
        flow.setMainWalletId(mainWallet.getId());
        flow.setAccountId(mainWallet.getAccountId());
        flow.setOrderNo(blankToNull(orderNo));
        flow.setBizType(bizType);
        flow.setDirection(direction);
        flow.setChangeAmount(defaultMoney(changeAmount));
        flow.setTotalBalanceBefore(defaultMoney(totalBefore));
        flow.setTotalBalanceAfter(defaultMoney(totalAfter));
        flow.setAvailableBalanceBefore(defaultMoney(availableBefore));
        flow.setAvailableBalanceAfter(defaultMoney(availableAfter));
        flow.setRemark(remark);
        customerMainWalletFlowMapper.insert(flow);
    }

    private void insertSubFlow(CustomerMainWallet mainWallet,
                               CustomerSubWallet subWallet,
                               String orderNo,
                               String bizType,
                               String direction,
                               BigDecimal changeAmount,
                               BigDecimal before,
                               BigDecimal after,
                               String remark) {
        CustomerSubWalletFlow flow = new CustomerSubWalletFlow();
        flow.setSubWalletId(subWallet.getId());
        flow.setMainWalletId(mainWallet.getId());
        flow.setAccountId(mainWallet.getAccountId());
        flow.setOrderNo(blankToNull(orderNo));
        flow.setWalletType(subWallet.getWalletType());
        flow.setBizType(bizType);
        flow.setDirection(direction);
        flow.setChangeAmount(defaultMoney(changeAmount));
        flow.setBalanceBefore(defaultMoney(before));
        flow.setBalanceAfter(defaultMoney(after));
        flow.setRemark(remark);
        customerSubWalletFlowMapper.insert(flow);
    }

    private BigDecimal normalizeMoney(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " is required");
        }
        BigDecimal normalized = amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, fieldName + " must be greater than 0");
        }
        return normalized;
    }

    private BigDecimal defaultMoney(BigDecimal amount) {
        return amount == null ? ZERO : amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateWalletNo(String prefix, Long accountId) {
        long suffix = ThreadLocalRandom.current().nextLong(1000, 9999);
        return prefix + accountId + WALLET_NO_TIME_FORMATTER.format(LocalDateTime.now()) + suffix;
    }

    public record BasicWalletDebitResult(WalletBundle walletBundle,
                                         BigDecimal debitedAmount,
                                         BigDecimal shortfallAmount) {

        public boolean overdraftApplied() {
            return shortfallAmount != null && shortfallAmount.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
