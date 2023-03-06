package com.xxxx.seckill.controller;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/goods")
public class GoodsController {
    @Autowired
    private IUserService userService;

    //@RequestMapping("/toList")
    //public String toList(HttpSession session, Model model, @CookieValue("userTicket") String ticket){
    //    // TODO：为什么这里可以有这样三个参数
    //    if(StringUtils.isEmpty(ticket)){
    //        return "login";
    //    }
    //     User user = (User) session.getAttribute(ticket);
    //    // 之前这种实际上称不上使用了分布式session，因为也不会再用到session
    //    // 只是把用户信息从session中提取出来，存到redis中去
    //    // 之前是把用户信息存到session里面去然后在不同地方获取session，
    //    // 现在直接存在redis里面去，所有应用都从该redis里面获取
    //    // 现在去maven配置里把springsession取消
    //    if(null == user){
    //        return "login";
    //    }
    //    model.addAttribute("user", user);
    //    return "goodsList";
    //    // 采用这种方式在多台服务器时会出问题，参考p13，或pdf: 分布式Session问题
    //}

    //@RequestMapping("/toList")
    //public String toList(HttpServletRequest request, HttpServletResponse response, Model model, @CookieValue("userTicket") String ticket){
    //    // TODO：为什么这里可以有这样session参数可以换成req与resp了？
    //    if(StringUtils.isEmpty(ticket)){
    //        return "login";
    //    }
    //    // 相当于有一个单独存放的服务器，不是到tomcat中找信息（参考文档），而是到redis里面去找信息
    //    User user = userService.getUserByCookie(ticket, request, response);
    //    if(null == user){
    //        return "login";
    //    }
    //    model.addAttribute("user", user);
    //    return "goodsList";
    //}

    @RequestMapping("/toList")
    public String toList(Model model, User user){
        model.addAttribute("user", user);
        return "goodsList";
    }
}
