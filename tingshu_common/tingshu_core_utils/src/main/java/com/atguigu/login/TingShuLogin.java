package com.atguigu.login;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
//生效范围
@Target({ElementType.METHOD})
//生命周期
@Retention(RetentionPolicy.RUNTIME)
public @interface TingShuLogin {
    boolean required() default true;
}
