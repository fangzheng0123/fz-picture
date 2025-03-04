package com.fz.fzpicturebackend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author fang
 * @Date 2025/2/26 22:51
 * @注释  自定义检测用户权限接口
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 必须具有某个角色
     */
    String mustRole() default "";
}
