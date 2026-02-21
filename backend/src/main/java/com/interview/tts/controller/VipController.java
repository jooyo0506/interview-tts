package com.interview.tts.controller;

import com.interview.tts.dto.ApiResponse;
import com.interview.tts.dto.VipOrderRequest;
import com.interview.tts.entity.*;
import com.interview.tts.repository.SysUserRepository;
import com.interview.tts.repository.VipOrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * VIP 控制器
 */
@RestController
@RequestMapping("/api/vip")
@RequiredArgsConstructor
public class VipController {

    private final SysUserRepository userRepository;
    private final VipOrderRepository orderRepository;

    /**
     * 获取VIP套餐列表
     */
    @GetMapping("/plans")
    public ApiResponse<List<Map<String, Object>>> getPlans() {
        List<Map<String, Object>> plans = Arrays.stream(VipPlan.values())
                .map(plan -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("type", plan.name());
                    map.put("name", plan.getName());
                    map.put("days", plan.getDays());
                    map.put("price", plan.getPrice());
                    map.put("charLimit", plan.getCharLimit());
                    return map;
                })
                .collect(Collectors.toList());
        return ApiResponse.success(plans);
    }

    /**
     * 创建VIP订单
     */
    @PostMapping("/create-order")
    public ApiResponse<VipOrder> createOrder(
            @RequestBody VipOrderRequest request,
            HttpServletRequest httpRequest) {
        SysUser user = (SysUser) httpRequest.getAttribute("currentUser");
        if (user == null) {
            return ApiResponse.error("请先登录");
        }

        VipPlan plan = VipPlan.fromType(request.getPlanType());

        VipOrder order = new VipOrder();
        order.setUserId(user.getId());
        order.setOrderNo(UUID.randomUUID().toString());
        order.setPlanType(plan.name());
        order.setAmount(new BigDecimal(plan.getPrice()));
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);
        return ApiResponse.success(order);
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/order-status")
    public ApiResponse<VipOrder> getOrderStatus(@RequestParam String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("订单不存在"));
    }

    /**
     * Mock 支付 (测试用)
     */
    @PostMapping("/mock-pay")
    public ApiResponse<VipOrder> mockPay(@RequestParam String orderNo, HttpServletRequest httpRequest) {
        SysUser user = (SysUser) httpRequest.getAttribute("currentUser");
        if (user == null) {
            return ApiResponse.error("请先登录");
        }

        VipOrder order = orderRepository.findByOrderNo(orderNo)
                .orElse(null);

        if (order == null) {
            return ApiResponse.error("订单不存在");
        }

        if (!order.getUserId().equals(user.getId())) {
            return ApiResponse.error("订单不属于当前用户");
        }

        // 更新订单状态
        order.setStatus("PAID");
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // 开通 VIP
        VipPlan plan = VipPlan.fromType(order.getPlanType());
        user.setUserType(UserType.VIP);

        if (user.getVipExpireDate() == null || user.getVipExpireDate().isBefore(LocalDateTime.now())) {
            user.setVipExpireDate(LocalDateTime.now().plusDays(plan.getDays()));
        } else {
            user.setVipExpireDate(user.getVipExpireDate().plusDays(plan.getDays()));
        }

        userRepository.save(user);

        return ApiResponse.success(order);
    }
}
