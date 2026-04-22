package site.xlinks.ai.router.client.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import site.xlinks.ai.router.client.dto.wallet.WalletMainFlowResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletSubAccountResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletSubFlowResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletSummaryResponse;
import site.xlinks.ai.router.client.dto.wallet.WalletWithdrawOrderResponse;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.common.enums.ErrorCode;
import site.xlinks.ai.router.common.exception.BusinessException;
import site.xlinks.ai.router.entity.CustomerMainWallet;
import site.xlinks.ai.router.entity.CustomerMainWalletFlow;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.entity.CustomerSubWalletFlow;
import site.xlinks.ai.router.mapper.CustomerMainWalletFlowMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;
import site.xlinks.ai.router.mapper.CustomerSubWalletFlowMapper;
import site.xlinks.ai.router.model.wallet.WalletBundle;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client wallet facade.
 */
@Service
@RequiredArgsConstructor
public class CustomerWalletFacadeService {

    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final WalletService walletService;
    private final CustomerMainWalletFlowMapper customerMainWalletFlowMapper;
    private final CustomerSubWalletFlowMapper customerSubWalletFlowMapper;
    private final CustomerOrderMapper customerOrderMapper;

    public WalletSummaryResponse getSummary(Long accountId) {
        WalletBundle bundle = walletService.ensureWallet(accountId);
        return toSummary(bundle);
    }

    public IPage<WalletMainFlowResponse> pageMainFlows(Long accountId, Integer page, Integer pageSize) {
        CustomerMainWallet mainWallet = walletService.ensureWallet(accountId).getMainWallet();
        Page<CustomerMainWalletFlow> flowPage = customerMainWalletFlowMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<CustomerMainWalletFlow>()
                        .eq(CustomerMainWalletFlow::getMainWalletId, mainWallet.getId())
                        .orderByDesc(CustomerMainWalletFlow::getCreatedAt)
                        .orderByDesc(CustomerMainWalletFlow::getId));
        Page<WalletMainFlowResponse> result = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        result.setRecords(flowPage.getRecords().stream().map(this::toMainFlowResponse).toList());
        return result;
    }

    public IPage<WalletSubFlowResponse> pageSubFlows(Long accountId, Integer page, Integer pageSize, String walletType) {
        CustomerMainWallet mainWallet = walletService.ensureWallet(accountId).getMainWallet();
        LambdaQueryWrapper<CustomerSubWalletFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomerSubWalletFlow::getMainWalletId, mainWallet.getId())
                .eq(StringUtils.hasText(walletType), CustomerSubWalletFlow::getWalletType, walletType == null ? null : walletType.trim())
                .orderByDesc(CustomerSubWalletFlow::getCreatedAt)
                .orderByDesc(CustomerSubWalletFlow::getId);
        Page<CustomerSubWalletFlow> flowPage = customerSubWalletFlowMapper.selectPage(new Page<>(page, pageSize), wrapper);
        Page<WalletSubFlowResponse> result = new Page<>(flowPage.getCurrent(), flowPage.getSize(), flowPage.getTotal());
        result.setRecords(flowPage.getRecords().stream().map(this::toSubFlowResponse).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public WalletWithdrawOrderResponse createWithdrawOrder(Long accountId, BigDecimal amount, String remark) {
        String orderNo = generateOrderNo("WD");
        String finalRemark = StringUtils.hasText(remark) ? remark.trim() : "Wallet withdraw requested";
        walletService.freezeFromBasic(accountId, amount, WalletConstants.BIZ_TYPE_WITHDRAW_FREEZE, orderNo, finalRemark);

        CustomerOrder order = new CustomerOrder();
        order.setOrderNo(orderNo);
        order.setAccountId(accountId);
        order.setOrderType(WalletConstants.ORDER_TYPE_WITHDRAW);
        order.setOrderTitle("Wallet Withdraw");
        order.setOrderInfo(buildWalletOrderInfo("withdraw", amount, remark));
        order.setPaymentChannel("manual");
        order.setTotalAmount(amount);
        order.setStatus(0);
        order.setRemark(finalRemark);
        customerOrderMapper.insert(order);
        return new WalletWithdrawOrderResponse(orderNo, "pending");
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelWithdrawOrder(Long accountId, String orderNo) {
        CustomerOrder order = loadWithdrawOrder(accountId, orderNo);
        if (order.getStatus() == null || order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "withdraw order cannot be cancelled");
        }
        walletService.unfreezeToBasic(accountId, order.getTotalAmount(), WalletConstants.BIZ_TYPE_WITHDRAW_CANCEL,
                order.getOrderNo(), "Withdraw request cancelled");

        CustomerOrder update = new CustomerOrder();
        update.setStatus(3);
        update.setCompleteAt(LocalDateTime.now());
        update.setRemark("Withdraw request cancelled");
        customerOrderMapper.update(update, new LambdaUpdateWrapper<CustomerOrder>()
                .eq(CustomerOrder::getId, order.getId())
                .eq(CustomerOrder::getStatus, 0));
    }

    public CustomerOrder loadWithdrawOrder(Long accountId, String orderNo) {
        CustomerOrder order = customerOrderMapper.selectOne(new LambdaQueryWrapper<CustomerOrder>()
                .eq(CustomerOrder::getOrderNo, orderNo)
                .eq(CustomerOrder::getAccountId, accountId)
                .eq(CustomerOrder::getOrderType, WalletConstants.ORDER_TYPE_WITHDRAW)
                .last("limit 1"));
        if (order == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "withdraw order not found");
        }
        return order;
    }

    private WalletSummaryResponse toSummary(WalletBundle bundle) {
        WalletSummaryResponse response = new WalletSummaryResponse();
        response.setWalletNo(bundle.getMainWallet().getWalletNo());
        response.setTotalBalance(bundle.getMainWallet().getTotalBalance());
        response.setAvailableBalance(bundle.getMainWallet().getAvailableBalance());
        response.setAllowIn(bundle.getMainWallet().getAllowIn());
        response.setAllowOut(bundle.getMainWallet().getAllowOut());
        response.setStatus(bundle.getMainWallet().getStatus());
        List<WalletSubAccountResponse> subWallets = bundle.getSubWallets().stream().map(item -> {
            WalletSubAccountResponse sub = new WalletSubAccountResponse();
            sub.setWalletNo(item.getWalletNo());
            sub.setWalletType(item.getWalletType());
            sub.setBalance(item.getBalance());
            sub.setStatus(item.getStatus());
            sub.setRemark(item.getRemark());
            return sub;
        }).toList();
        response.setSubWallets(subWallets);
        return response;
    }

    private WalletMainFlowResponse toMainFlowResponse(CustomerMainWalletFlow item) {
        WalletMainFlowResponse response = new WalletMainFlowResponse();
        response.setId(item.getId());
        response.setOrderNo(item.getOrderNo());
        response.setBizType(item.getBizType());
        response.setDirection(item.getDirection());
        response.setChangeAmount(item.getChangeAmount());
        response.setTotalBalanceBefore(item.getTotalBalanceBefore());
        response.setTotalBalanceAfter(item.getTotalBalanceAfter());
        response.setAvailableBalanceBefore(item.getAvailableBalanceBefore());
        response.setAvailableBalanceAfter(item.getAvailableBalanceAfter());
        response.setRemark(item.getRemark());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private WalletSubFlowResponse toSubFlowResponse(CustomerSubWalletFlow item) {
        WalletSubFlowResponse response = new WalletSubFlowResponse();
        response.setId(item.getId());
        response.setOrderNo(item.getOrderNo());
        response.setWalletType(item.getWalletType());
        response.setBizType(item.getBizType());
        response.setDirection(item.getDirection());
        response.setChangeAmount(item.getChangeAmount());
        response.setBalanceBefore(item.getBalanceBefore());
        response.setBalanceAfter(item.getBalanceAfter());
        response.setRemark(item.getRemark());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    private String buildWalletOrderInfo(String scene, BigDecimal amount, String remark) {
        String escapedRemark = remark == null ? "" : remark.replace("\"", "\\\"");
        return "{\"scene\":\"" + scene + "\",\"amount\":\"" + amount + "\",\"remark\":\"" + escapedRemark + "\"}";
    }

    private String generateOrderNo(String prefix) {
        return prefix + ORDER_TIME_FORMATTER.format(LocalDateTime.now()) + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
