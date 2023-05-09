package com.xxxx.seckill.config;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @description https://www.cnblogs.com/hligy/p/16647209.html
 * @param null
 * defaultParamDescription
 * @author wangjunyou
 * @date 2023/5/9 22:24
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface RateLimiter {
    TimeUnit unit();

    long time();

    long limit();
}
