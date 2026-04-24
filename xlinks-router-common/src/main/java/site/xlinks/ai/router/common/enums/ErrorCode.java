package site.xlinks.ai.router.common.enums;

/**
 * Common business error codes.
 */
public enum ErrorCode {

    SUCCESS(0, "Success"),

    PARAM_ERROR(4001, "Parameter error"),
    CUSTOMER_TOKEN_INVALID(4002, "Customer token invalid"),
    MODEL_UNAVAILABLE(4003, "Model unavailable"),
    PROVIDER_UNAVAILABLE(4004, "Provider unavailable"),
    PROVIDER_TOKEN_UNAVAILABLE(4005, "Provider token unavailable"),
    MODEL_NOT_IN_ALLOWED_LIST(4006, "Model not allowed"),
    TOKEN_EXPIRED(4007, "Token expired"),
    SMS_CODE_INVALID(4008, "SMS code invalid"),
    USER_ALREADY_EXISTS(4009, "User already exists"),
    UNAUTHORIZED(4010, "Unauthorized"),
    FORBIDDEN(4011, "Forbidden"),
    ROUTE_ERROR(4012, "Route error"),
    USER_NOT_FOUND(4013, "User not found"),
    ACCOUNT_DISABLED(4014, "Account disabled"),
    VERIFY_CODE_ERROR(4015, "Verify code error"),
    PHONE_ALREADY_EXISTS(4016, "Phone already exists"),
    EMAIL_ALREADY_EXISTS(4017, "Email already exists"),
    PASSWORD_ERROR(4018, "Password error"),
    USER_DISABLED(4019, "User disabled"),
    INVITE_CODE_INVALID(4020, "Invite code invalid"),
    RATE_LIMITED(4021, "Rate limited"),

    SYSTEM_ERROR(5000, "System error"),
    EXTERNAL_SERVICE_ERROR(5001, "External service error"),
    INTERNAL_ERROR(5002, "Internal error"),
    UPSTREAM_TIMEOUT(5003, "Upstream timeout");

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
