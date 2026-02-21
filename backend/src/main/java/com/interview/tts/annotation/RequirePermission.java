package com.interview.tts.annotation;

import com.interview.tts.entity.UserType;
import java.lang.annotation.*;

/**
 * 权限注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 需要的用户类型
     */
    UserType value() default UserType.USER;
}
