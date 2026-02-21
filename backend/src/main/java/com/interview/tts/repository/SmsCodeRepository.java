package com.interview.tts.repository;

import com.interview.tts.entity.SmsCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SmsCodeRepository extends JpaRepository<SmsCode, Long> {
    Optional<SmsCode> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
