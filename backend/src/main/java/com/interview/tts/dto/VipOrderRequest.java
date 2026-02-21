package com.interview.tts.dto;

import lombok.Data;

/**
 * VIP 订单请求
 */
@Data
public class VipOrderRequest {
    /**
     * 套餐类型: MONTHLY, YEARLY, LIFETIME
     */
    private String planType;
}
