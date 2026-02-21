package com.interview.tts.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * VIP 套餐
 */
@Getter
@AllArgsConstructor
public enum VipPlan {
    MONTHLY(30, "月度会员", 30, 30),
    YEARLY(365, "年度会员", 300, 3650),
    LIFETIME(3650, "终身会员", 1000, 36500);

    /**
     * 天数
     */
    private final int days;

    /**
     * 名称
     */
    private final String name;

    /**
     * 价格(分)
     */
    private final int price;

    /**
     * 包含字数
     */
    private final int charLimit;

    public static VipPlan fromType(String planType) {
        for (VipPlan plan : values()) {
            if (plan.name().equals(planType)) {
                return plan;
            }
        }
        return MONTHLY;
    }
}
