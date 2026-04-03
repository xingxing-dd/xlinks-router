package site.xlinks.ai.router.common.enums;

import lombok.Getter;

/**
 * 联系我们主题枚举。
 */
@Getter
public enum ContactSubjectEnum {

    TECHNICAL("technical", "技术支持"),
    BILLING("billing", "账单问题"),
    FEATURE("feature", "功能建议"),
    BUG("bug", "Bug 反馈"),
    OTHER("other", "其他问题");

    private final String code;
    private final String description;

    ContactSubjectEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static boolean contains(String code) {
        for (ContactSubjectEnum subject : values()) {
            if (subject.code.equals(code)) {
                return true;
            }
        }
        return false;
    }
}