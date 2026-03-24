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
    SMS_CODE_INVALID(4008, "短信验证码错误或已失效"),
    USER_ALREADY_EXISTS(4009, "用户已存在"),
    UNAUTHORIZED(4010, "未授权"),
    FORBIDDEN(4011, "无权限"),
    ROUTE_ERROR(4012, "路由错误"),
    USER_NOT_FOUND(4013, "用户不存在"),
    ACCOUNT_DISABLED(4014, "账号已禁用"),
    VERIFY_CODE_ERROR(4015, "验证码错误"),
    PHONE_ALREADY_EXISTS(4016, "手机号已被注册"),
    EMAIL_ALREADY_EXISTS(4017, "邮箱已被注册"),
    PASSWORD_ERROR(4018, "密码错误"),
    USER_DISABLED(4019, "用户已被禁用"),
    INVITE_CODE_INVALID(4020, "邀请码无效"),

    // 服务端错误 5xxx
    SYSTEM_ERROR(5000, "系统异常"),
    EXTERNAL_SERVICE_ERROR(5001, "外部服务调用失败"),
    INTERNAL_ERROR(5002, "内部错误");

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
