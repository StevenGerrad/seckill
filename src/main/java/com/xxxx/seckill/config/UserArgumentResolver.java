package com.xxxx.seckill.config;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.utils.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component  // 添加Component注解，因为要注入到webConfig中
public class UserArgumentResolver implements HandlerMethodArgumentResolver {
    @Autowired
    private IUserService userService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 返回为true才会执行 resolveArgument
        Class<?> clazz = parameter.getParameterType();
        return clazz == User.class;
    }

    // @Override
    // public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    //     HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
    //     HttpServletResponse response = webRequest.getNativeResponse(HttpServletResponse.class);
    //     // P63 注释掉，同样的功能在AccessLimit中有实现了
    //     String ticket = CookieUtil.getCookieValue(request, "userTicket");
    //     if(StringUtils.isEmpty(ticket)){
    //         return null;
    //     }
    //     return userService.getUserByCookie(ticket, request, response);
    // }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return UserContext.getUser();
    }
}
