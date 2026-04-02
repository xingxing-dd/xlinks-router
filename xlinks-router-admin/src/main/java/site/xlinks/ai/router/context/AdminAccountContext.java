package site.xlinks.ai.router.context;

import site.xlinks.ai.router.entity.AdminAccount;

/**
 * Admin account request context.
 */
public final class AdminAccountContext {

    private static final ThreadLocal<AdminAccount> ACCOUNT_THREAD_LOCAL = new ThreadLocal<>();

    private AdminAccountContext() {
    }

    public static void setAccount(AdminAccount account) {
        ACCOUNT_THREAD_LOCAL.set(account);
    }

    public static AdminAccount getAccount() {
        return ACCOUNT_THREAD_LOCAL.get();
    }

    public static AdminAccount requireAccount() {
        AdminAccount account = getAccount();
        if (account == null) {
            throw new IllegalStateException("Admin account not found in request context");
        }
        return account;
    }

    public static Long getAccountId() {
        AdminAccount account = getAccount();
        return account != null ? account.getId() : null;
    }

    public static void clear() {
        ACCOUNT_THREAD_LOCAL.remove();
    }
}
