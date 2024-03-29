package com.xxxx.seckill.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableWebMvc     // 【弹幕调bug】EnableWebMvc会拦截静态资源，要注释掉 有的说可以addResourceHandler 配置一下
public class WebConfig implements WebMvcConfigurer {
    // 配置的优先级高于配置文件


    // IDEA：这里可以ctrl + O 调出窗口"Select Methods to Overwrite/implement"

    @Autowired
    private UserArgumentResolver userArgumentResolver;
    @Autowired
    private AccessLimitInterceptor accessLimitInterceptor;
    @Autowired
    private RateLimiterHandlerInterceptor rateLimiterHandlerInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 注意看 HandlerMethodArgumentResolver 是一个接口

        WebMvcConfigurer.super.addArgumentResolvers(resolvers);

        resolvers.add(userArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        WebMvcConfigurer.super.addResourceHandlers(registry);

        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLimitInterceptor);
        registry.addInterceptor(rateLimiterHandlerInterceptor);
    }
}
