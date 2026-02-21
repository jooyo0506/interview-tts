package com.interview.tts.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * VIP 订单
 */
@Data
@Entity
@Table(name = "vip_order")
public class VipOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_no", unique = true, nullable = false, length = 64)
    private String orderNo;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
