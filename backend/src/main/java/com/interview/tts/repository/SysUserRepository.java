package com.interview.tts.repository;

import com.interview.tts.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    Optional<SysUser> findByUserKey(String userKey);
    Optional<SysUser> findByPhone(String phone);
}
