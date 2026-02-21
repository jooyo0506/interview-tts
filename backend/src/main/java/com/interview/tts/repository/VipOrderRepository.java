package com.interview.tts.repository;

import com.interview.tts.entity.VipOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface VipOrderRepository extends JpaRepository<VipOrder, Long> {
    Optional<VipOrder> findByOrderNo(String orderNo);
}
