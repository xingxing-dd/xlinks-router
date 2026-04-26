package site.xlinks.ai.router.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.xlinks.ai.router.client.config.PromotionProperties;
import site.xlinks.ai.router.common.constants.WalletConstants;
import site.xlinks.ai.router.entity.CustomerAccount;
import site.xlinks.ai.router.entity.CustomerOrder;
import site.xlinks.ai.router.entity.PromotionRecord;
import site.xlinks.ai.router.mapper.CustomerAccountMapper;
import site.xlinks.ai.router.mapper.CustomerOrderMapper;
import site.xlinks.ai.router.mapper.PromotionRecordMapper;
import site.xlinks.ai.router.mapper.PromotionRuleMapper;
import site.xlinks.ai.router.service.WalletService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private CustomerAccountMapper customerAccountMapper;

    @Mock
    private CustomerOrderMapper customerOrderMapper;

    @Mock
    private PromotionRecordMapper promotionRecordMapper;

    @Mock
    private PromotionRuleMapper promotionRuleMapper;

    @Mock
    private WalletService walletService;

    private PromotionService promotionService;

    @BeforeEach
    void setUp() {
        PromotionProperties properties = new PromotionProperties();
        properties.setReferralLinkPrefix("https://token.example/register?ref=");
        promotionService = new PromotionService(
                customerAccountMapper,
                customerOrderMapper,
                promotionRecordMapper,
                promotionRuleMapper,
                properties,
                walletService
        );
    }

    @Test
    void shouldSettleFixedRewardForFirstSuccessfulRecharge() {
        CustomerAccount invitee = new CustomerAccount();
        invitee.setId(2001L);
        invitee.setInvitedBy(1001L);

        CustomerAccount inviter = new CustomerAccount();
        inviter.setId(1001L);
        inviter.setInviteCode("INVITE88");

        CustomerOrder order = new CustomerOrder();
        order.setOrderNo("RECH001");
        order.setAccountId(2001L);
        order.setOrderType(WalletConstants.ORDER_TYPE_RECHARGE);
        order.setStatus(1);

        when(customerAccountMapper.selectById(2001L)).thenReturn(invitee);
        when(customerAccountMapper.selectById(1001L)).thenReturn(inviter);
        when(promotionRecordMapper.selectCount(any())).thenReturn(0L);
        when(customerOrderMapper.selectCount(any())).thenReturn(1L);
        when(customerOrderMapper.selectOne(any())).thenReturn(order);

        promotionService.createFirstRechargeReward(2001L, new BigDecimal("100.00"), "RECH001");

        verify(walletService).creditBasic(
                1001L,
                new BigDecimal("5.00"),
                "promotion_reward",
                "RECH001",
                "Referral reward for first successful recharge"
        );

        ArgumentCaptor<PromotionRecord> recordCaptor = ArgumentCaptor.forClass(PromotionRecord.class);
        verify(promotionRecordMapper).insert(recordCaptor.capture());
        PromotionRecord record = recordCaptor.getValue();
        assertEquals(1001L, record.getInviterUserId());
        assertEquals(2001L, record.getInviteeUserId());
        assertEquals(new BigDecimal("5.00"), record.getRewardAmount());
        assertEquals(1, record.getStatus());
        assertEquals("RECH001", record.getSourceOrderNo());
        assertNotNull(record.getSettleAt());
    }

    @Test
    void shouldSkipRewardWhenRechargeIsNotFirstSuccessfulOne() {
        CustomerAccount invitee = new CustomerAccount();
        invitee.setId(2001L);
        invitee.setInvitedBy(1001L);

        when(customerAccountMapper.selectById(2001L)).thenReturn(invitee);
        when(promotionRecordMapper.selectCount(any())).thenReturn(0L);
        when(customerOrderMapper.selectCount(any())).thenReturn(2L);

        promotionService.createFirstRechargeReward(2001L, new BigDecimal("100.00"), "RECH002");

        verify(walletService, never()).creditBasic(any(), any(), any(), any(), any());
        verify(promotionRecordMapper, never()).insert(any(PromotionRecord.class));
    }
}
