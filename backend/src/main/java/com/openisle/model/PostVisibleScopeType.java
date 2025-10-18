package com.openisle.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PostVisibleScopeType {
    ALL,
    ONLY_ME,
    ONLY_REGISTER;

    /**
     * 防止画面传递错误的值
     * @param value
     * @return
     */
    @JsonCreator
    public static PostVisibleScopeType fromString(String value) {
        if (value == null) return ALL;
        for (PostVisibleScopeType type : PostVisibleScopeType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        // 不匹配时给默认值，而不是抛异常
        return ALL;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
