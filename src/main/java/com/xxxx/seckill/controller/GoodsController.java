package com.xxxx.seckill.controller;

import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IUserService;
import com.xxxx.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.concurrent.TimeUnit;

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

    //@RequestMapping("/toList")
    //public String toList(Model model, User user){
    //    model.addAttribute("user", user);
    //    return "goodsList";
    //}

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private RedisTemplate redisTemplate;

    // 做手动渲染
    @Autowired
    private ThymeleafViewResolver thymeleafViewResolver;

    /**
     * @description defaultDescription
     * windows 优化前(1000 kernal * 3) QPS: 909.4
     * windows p38 优化(1000 kernal * 3) QPS: 3722.4
     * windows 优化前(5000 kernal * 3) QPS: 1247
     * windows p38 优化(5000 kernal * 3) QPS: 4519
     * @param model
     * defaultParamDescription
     * @param user
     * defaultParamDescription
     * @return String
     * @author wangjunyou
     * @date 2023/4/29 15:49
     */
    // @RequestMapping("/toList")
    // public String toList(Model model, User user){
    //     model.addAttribute("user", user);
    //     model.addAttribute("goodsList", goodsService.findGoodsVo());
    //     return "goodsList";
    // }

    @RequestMapping(value="/toList", produces="text/html;charset=utf-8")
    @ResponseBody
    public String toList(Model model, User user,
                         HttpServletRequest request, HttpServletResponse response) {
        // Redis中获取页面，如果不为空，直接返回页面
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String html = (String) valueOperations.get("goodsList");
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        model.addAttribute("user", user);
        model.addAttribute("goodsList", goodsService.findGoodsVo());
        // return "goodsList";
        // 如果为空，手动渲染，存入Redis并返回
        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(),
                model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsList", context);
        // 设置失效时间
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsList", html, 60, TimeUnit.SECONDS);
        }
        return html;

    }

    /*@RequestMapping("/toDetail/{goodsId}")
    public String toDetail(Model model, User user, @PathVariable Long goodsId){
        model.addAttribute("user", user);

        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        // 秒杀状态
        int secKillStatus = 0;
        // 秒杀倒计时
        int remainSeconds = 0;
        // 秒杀还未开始
        if (nowDate.before(startDate)) {
            remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000));
        } else if (nowDate.after(endDate)) {
            // 秒杀已结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            // 秒杀中
            secKillStatus = 1;
            remainSeconds = 0;
        }

        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("goods", goodsService.findGoodsVoByGoodsId(goodsId));
        return "goodsDetail";
    }*/

    @RequestMapping(value = "/toDetail/{goodsId}", produces = "text/html;charset=utf-8")
    @ResponseBody
    public String toDetail2(Model model, User user, @PathVariable Long goodsId,
                            HttpServletRequest request, HttpServletResponse response) {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //Redis中获取页面，如果不为空，直接返回页面
        String html = (String) valueOperations.get("goodsDetail:" + goodsId);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        //秒杀还未开始
        if (nowDate.before(startDate)) {
            remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000));
        } else if (nowDate.after(endDate)) {
            //	秒杀已结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀中
            secKillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("goods", goodsVo);
        WebContext context = new WebContext(request, response, request.getServletContext(), request.getLocale(),
                model.asMap());
        html = thymeleafViewResolver.getTemplateEngine().process("goodsDetail", context);
        if (!StringUtils.isEmpty(html)) {
            valueOperations.set("goodsDetail:" + goodsId, html, 60, TimeUnit.SECONDS);
        }
        return html;
        // return "goodsDetail";
    }
}
