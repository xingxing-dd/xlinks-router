package site.xlinks.ai.router.client.context;

import site.xlinks.ai.router.entity.MerchantAccount;

/**
 * 商户账户上下文
 * 使用ThreadLocal存储当前请求的商户账户信息
 */
public class MerchantAccountContext {

    private static final ThreadLocal<MerchantAccount> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的商户账户
     */
    public static void setAccount(MerchantAccount account) {
        ACCOUNT_THREAD_LOCAL.set(account);
    }

    /**
     * 获取当前线程的商户账户
     */
    public static MerchantAccount getAccount() {
        return ACCOUNT_THREAD_LOCAL.get();
    }

    /**
     * 获取当前商户账户ID
     */
    public static Long getAccountId() {
        MerchantAccount account = getAccount();
        return account != null ? account.getId() : null;
    }

    /**
     * 清除当前线程的商户账户
     */
    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
    }
}
