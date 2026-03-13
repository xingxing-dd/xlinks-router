package site.xlinks.ai.router.common.enums;

/**
 * 错误码枚举
 */
public enum ErrorCode {

    // 成功
    SUCCESS(0, "成功"),

    // 客户端错误 4xxx
    PARAM_ERROR(4001, "参数错误"),
    CUSTOMER_TOKEN_INVALID(4002, "Customer Token 无效"),
    MODEL_UNAVAILABLE(4003, "模型不可用"),
    PROVIDER_UNAVAILABLE(4004, "Provider 不可用"),
    PROVIDER_TOKEN_UNAVAILABLE(4005, "Provider Token 不可用"),
    MODEL_NOT_IN_ALLOWED_LIST(4006, "模型不在允许列表中"),
    TOKEN_EXPIRED(4007, "Token 已过期"),

    // 服务端错误 5xxx
    SYSTEM_ERROR(5000, "系统异常"),
    EXTERNAL_SERVICE_ERROR(5001, "外部服务调用失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
