package site.xlinks.ai.router.client.context;

import site.xlinks.ai.router.entity.CustomerAccount;

/**
 * 客户账户上下文
 */
public class CustomerAccountContext {

    private static final ThreadLocal<CustomerAccount> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前线程的账户信息
     */
    public static void setAccount(CustomerAccount account) {
        ACCOUNT_THREAD_LOCAL.set(account);
    }

    /**
     * 获取当前线程的账户信息
     */
    public static CustomerAccount getAccount() {
        return ACCOUNT_THREAD_LOCAL.get();
    }

    /**
     * 获取当前账户ID
     */
    public static Long getAccountId() {
        CustomerAccount account = getAccount();
        return account != null ? account.getId() : null;
    }

    /**
     * 清理当前线程的账户信息
     */
    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
    }
}
