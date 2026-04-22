package site.xlinks.ai.router.common.constants;

/**
 * Wallet domain constants.
 */
public final class WalletConstants {

    private WalletConstants() {
    }

    public static final String SUB_WALLET_BASIC = "basic";
    public static final String SUB_WALLET_FROZEN = "frozen";
    public static final String SUB_WALLET_PENDING_SETTLEMENT = "pending_settlement";

    public static final String FLOW_DIRECTION_IN = "in";
    public static final String FLOW_DIRECTION_OUT = "out";
    public static final String FLOW_DIRECTION_TRANSFER = "transfer";

    public static final String ORDER_TYPE_RECHARGE = "recharge";
    public static final String ORDER_TYPE_WITHDRAW = "withdraw";

    public static final String BIZ_TYPE_WALLET_OPEN = "wallet_open";
    public static final String BIZ_TYPE_MANUAL_CREDIT = "manual_credit";
    public static final String BIZ_TYPE_MANUAL_DEBIT = "manual_debit";
    public static final String BIZ_TYPE_RECHARGE = "recharge";
    public static final String BIZ_TYPE_API_USAGE = "api_usage";
    public static final String BIZ_TYPE_WITHDRAW_FREEZE = "withdraw_freeze";
    public static final String BIZ_TYPE_WITHDRAW_CANCEL = "withdraw_cancel";
    public static final String BIZ_TYPE_WITHDRAW_SUCCESS = "withdraw_success";
    public static final String BIZ_TYPE_PENDING_SETTLEMENT_IN = "pending_settlement_in";
    public static final String BIZ_TYPE_SETTLEMENT = "settlement";
    public static final String BIZ_TYPE_FREEZE = "freeze";
    public static final String BIZ_TYPE_UNFREEZE = "unfreeze";
}
