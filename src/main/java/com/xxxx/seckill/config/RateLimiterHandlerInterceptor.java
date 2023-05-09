package com.xxxx.seckill.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.utils.CookieUtil;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.caffeine.CaffeineProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.remote.RemoteBucketState;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterHandlerInterceptor implements HandlerInterceptor {

    @Autowired
    private IUserService userService;
    private final ProxyManager<String> proxyManager;

    public RateLimiterHandlerInterceptor() {
        Caffeine<String, RemoteBucketState> builder = Caffeine.newBuilder()
                .removalListener((key, graph, cause) -> {});
        proxyManager = new CaffeineProxyManager<>(builder, Duration.ofMinutes(5));
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            // 获取当前登录用户
            User user = getUser(request, response);
            UserContext.setUser(user);
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RateLimiter rateLimiter = handlerMethod.getMethodAnnotation(RateLimiter.class);
            String key = request.getRequestURI();

            // 必须要进行登录
            if (user == null) {
                // 构建返回对象
                render(response, RespBeanEnum.SESSION_ERROR);
                return false;
            }
            key += ":" + user.getId();

            if (rateLimiter != null) {
                Bucket bucket = proxyManager.builder().build(key, () -> bucketConfigurationByRateLimiter(rateLimiter));
                if (!bucket.tryConsume(1)) {
                    // response.setStatus(429);
                    render(response, RespBeanEnum.ACCESS_LIMIT_REAHCED);
                    return false;
                }
            }
        }
        return true;
    }

    private BucketConfiguration bucketConfigurationByRateLimiter(RateLimiter rateLimiter) {
        TimeUnit timeUnit = rateLimiter.unit();
        long time = rateLimiter.time();
        long limit = rateLimiter.limit();
        Bandwidth bandwidth = Bandwidth.simple(limit, TimeUnit.SECONDS.equals(timeUnit) ? Duration.ofSeconds(time) : Duration.ofMinutes(time));
        return BucketConfiguration.builder()
                .addLimit(bandwidth)
                .build();
    }

    private void render(HttpServletResponse response, RespBeanEnum respBeanEnum) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        RespBean respBean = RespBean.error(respBeanEnum);
        out.write(new ObjectMapper().writeValueAsString(respBean));
        out.flush();
        out.close();
    }

    private User getUser(HttpServletRequest request, HttpServletResponse response) {
        String ticket = CookieUtil.getCookieValue(request, "userTicket");
        if (StringUtils.isEmpty(ticket)) {
            return null;
        }
        return userService.getUserByCookie(ticket, request, response);
    }
}
