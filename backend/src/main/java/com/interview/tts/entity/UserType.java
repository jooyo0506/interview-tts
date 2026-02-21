package com.interview.tts.entity;

/**
 * 用户类型枚举
 */
public enum UserType {
    USER("普通用户"),
    VIP("VIP会员");

    private final String desc;

    UserType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
