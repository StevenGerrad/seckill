package com.xxxx.seckill.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wf.captcha.ArithmeticCaptcha;
import com.xxxx.seckill.config.AccessLimit;
import com.xxxx.seckill.config.RateLimiter;
import com.xxxx.seckill.exception.GlobalException;
import com.xxxx.seckill.pojo.Order;
import com.xxxx.seckill.pojo.SeckillMessage;
import com.xxxx.seckill.pojo.SeckillOrder;
import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.service.IOrderService;
import com.xxxx.seckill.service.ISeckillOrderService;
import com.xxxx.seckill.utils.JsonUtil;
import com.xxxx.seckill.vo.GoodsVo;
import com.xxxx.seckill.vo.RespBean;
import com.xxxx.seckill.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Controller
@RequestMapping("/seckill")
public class SecKillController implements InitializingBean {
    @Autowired
    private IGoodsService goodsService;
    @Autowired
    private ISeckillOrderService seckillOrderService;

    @Autowired
    private IOrderService orderService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private MQSender mqSender;

    @Autowired
    private RedisScript<Long> script;

    private Map<Long, Boolean> EmptyStockMap = new HashMap<>();




    /**
     * @description
     * windows 优化前(1000 kernal * 1) QPS: 30.3
     * windows 优化前(1000 kernal * 3) QPS: 29.9 (注意要设置循环数，才会出现库存为负数的问题)
     *         缓存优化(1000 * 3) QPS: 556.8
     *         接口优化(1000 * 3) QPS: 1048.6
     * @param model
     * defaultParamDescription
     * @param user
     * defaultParamDescription
     * @param goodsId
     * defaultParamDescription
     * @return String
     * @author Administrator
     * @date 2023/4/22 21:51
     */
    @RequestMapping("/doSeckill2")
    public String doSecKill2(Model model, User user, Long goodsId){
        if(user == null){
            return "login";
        }
        model.addAttribute("user", user);
        // TODO：这里不是应该查询秒杀商品的库存吗
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        // 判断库存
        if(goods.getStockCount() < 1){
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        // 判断是否重复抢购
        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id",
                user.getId()).eq("goods_id", goodsId));
        if(seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }
        // TODO: 这里难道不需要像 toDetail 一样判断一下秒杀状态吗？被跳过 toDetail 直接攻击这个接口怎么办？
        Order order = orderService.seckill(user, goods);
        model.addAttribute("order", order);
        model.addAttribute("goods", goods);

        // TODO：看一下这个spring ui Model 是啥，怎么什么都往里加
        return "orderDetail";
    }

    // @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    // @ResponseBody
    // public RespBean doSecKill(Model model, User user, Long goodsId){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //     GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
    //     // 判断库存
    //     if(goods.getStockCount() < 1){
    //         model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     // 判断是否重复抢购
    //     SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id",
    //             user.getId()).eq("goods_id", goodsId));
    //     if(seckillOrder != null) {
    //         model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
    //         return RespBean.error(RespBeanEnum.REPEATE_ERROR);
    //     }
    //     Order order = orderService.seckill(user, goods);
    //
    //     return RespBean.success(order);
    // }

    // @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    // @ResponseBody
    // public RespBean doSecKill(Model model, User user, Long goodsId){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //     GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
    //     // 判断库存
    //     if(goods.getStockCount() < 1){
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     // 判断是否重复抢购
    //     // SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id",
    //     //         user.getId()).eq("goods_id", goodsId));
    //     SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goods.getId());
    //     if(seckillOrder != null) {
    //         return RespBean.error(RespBeanEnum.REPEATE_ERROR);
    //     }
    //     Order order = orderService.seckill(user, goods);
    //
    //     return RespBean.success(order);
    // }

    // P52
    // @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    // @ResponseBody
    // public RespBean doSecKill(Model model, User user, Long goodsId){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //     ValueOperations valueOperations = redisTemplate.opsForValue();
    //     // 判断是否重复抢购
    //     SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
    //     if(seckillOrder != null) {
    //         return RespBean.error(RespBeanEnum.REPEATE_ERROR);
    //     }
    //     if(EmptyStockMap.get(goodsId)){
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     // 预减库存
    //     // TODO：原本的decrement满足原子性吗？
    //     Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
    //     if(stock < 0){
    //         EmptyStockMap.put(goodsId, true);
    //         valueOperations.increment("seckillGoods:" + goodsId);
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
    //     // 使用RabbitMq可以达到流量削峰的效果
    //     mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
    //
    //     return RespBean.success(0);
    // }

    // P56
    // @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    // @ResponseBody
    // public RespBean doSecKill(Model model, User user, Long goodsId){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //     ValueOperations valueOperations = redisTemplate.opsForValue();
    //     // 判断是否重复抢购
    //     SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
    //     if(seckillOrder != null) {
    //         return RespBean.error(RespBeanEnum.REPEATE_ERROR);
    //     }
    //     if(EmptyStockMap.get(goodsId)){
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     // 预减库存
    //     // Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
    //     Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
    //     if(stock < 0){
    //         EmptyStockMap.put(goodsId, true);
    //         valueOperations.increment("seckillGoods:" + goodsId);
    //         return RespBean.error(RespBeanEnum.EMPTY_STOCK);
    //     }
    //     SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
    //     // 使用RabbitMq可以达到流量削峰的效果
    //     mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));
    //
    //     return RespBean.success(0);
    // }

    @Override
    public void afterPropertiesSet() throws Exception{
        // 将库存加载到redis
        List<GoodsVo> list = goodsService.findGoodsVo();
        if(CollectionUtils.isEmpty(list)) return ;
        list.forEach(goodsVo -> {
            redisTemplate.opsForValue().set("seckillGoods:" + goodsVo.getId(), goodsVo.getStockCount());
            EmptyStockMap.put(goodsVo.getId(), false);
        });
    }

    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getResult(User user, Long goodsId){
        if(user == null) return RespBean.error(RespBeanEnum.SESSION_ERROR);
        Long orderId = seckillOrderService.getResult(user, goodsId);
        return RespBean.success(orderId);
    }

    // @RequestMapping(value = "/path", method = RequestMethod.GET)
    // @ResponseBody
    // public RespBean getPath(User user, Long goodsId, String captcha){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //     boolean check = orderService.checkCaptcha(user, goodsId, captcha);
    //     if(!check){
    //         return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
    //     }
    //     String str = orderService.createPath(user, goodsId);
    //     return RespBean.success(str);
    // }

    // @RequestMapping(value = "/path", method = RequestMethod.GET)
    // @ResponseBody
    // public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request){
    //     if(user == null){
    //         return RespBean.error(RespBeanEnum.SESSION_ERROR);
    //     }
    //
    //     // P62 简单接口限流
    //     ValueOperations valueOperations = redisTemplate.opsForValue();
    //     // 计数器方式：限制访问次数，5秒内访问5次
    //     // 一般设置为最大能够承受QPS的70%-80%
    //     // 问题：在两个时间区间的连接处大量涌入，还是实质上会压力过大，同时时间区间的部分时间段实质上空闲，浪费资源
    //     String uri = request.getRequestURI();
    //     captcha = "0";
    //     // TODO：这个对吗？uri就是"/seckill/path:13000000258"
    //     Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
    //     if(count == null){
    //         valueOperations.set(uri + ":" + user.getId(), 1, 5, TimeUnit.SECONDS);
    //     } else if(count < 5){
    //         valueOperations.increment(uri + ":" + user.getId());
    //     } else{
    //         return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REAHCED);
    //     }
    //     // 漏斗方式：保护他人
    //     // 令牌桶算法：保护自己
    //     // 同时这段代码需要在每个接口都拷一遍——拦截器
    //
    //     boolean check = orderService.checkCaptcha(user, goodsId, captcha);
    //     if(!check){
    //         return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
    //     }
    //     String str = orderService.createPath(user, goodsId);
    //     return RespBean.success(str);
    // }

    // @AccessLimit(second = 5, maxCount=5, needLogin=true)
    @RateLimiter(unit = TimeUnit.SECONDS, time = 10, limit = 5)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public RespBean getPath(User user, Long goodsId, String captcha, HttpServletRequest request){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }

        // // P62 简单接口限流
        // ValueOperations valueOperations = redisTemplate.opsForValue();
        // // 计数器方式：限制访问次数，5秒内访问5次
        // // 一般设置为最大能够承受QPS的70%-80%
        // // 问题：在两个时间区间的连接处大量涌入，还是实质上会压力过大，同时时间区间的部分时间段实质上空闲，浪费资源
        // String uri = request.getRequestURI();
        // captcha = "0";
        // // TODO：这个对吗？uri就是"/seckill/path:13000000258"
        // Integer count = (Integer) valueOperations.get(uri + ":" + user.getId());
        // if(count == null){
        //     valueOperations.set(uri + ":" + user.getId(), 1, 5, TimeUnit.SECONDS);
        // } else if(count < 5){
        //     valueOperations.increment(uri + ":" + user.getId());
        // } else{
        //     return RespBean.error(RespBeanEnum.ACCESS_LIMIT_REAHCED);
        // }
        // // 漏斗方式：保护他人
        // // 令牌桶算法：保护自己
        // // 同时这段代码需要在每个接口都拷一遍——拦截器

        boolean check = orderService.checkCaptcha(user, goodsId, captcha);
        if(!check){
            return RespBean.error(RespBeanEnum.ERROR_CAPTCHA);
        }
        String str = orderService.createPath(user, goodsId);
        return RespBean.success(str);
    }

    // P59
    @RequestMapping(value = "/{path}/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill(@PathVariable String path, User user, Long goodsId){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();

        // P59，校验秒杀地址
        boolean check = orderService.checkPath(user, goodsId, path);
        if(!check){
            return RespBean.error(RespBeanEnum.REQUEST_ILLEGAL);
        }

        // 判断是否重复抢购
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if(seckillOrder != null) {
            return RespBean.error(RespBeanEnum.REPEATE_ERROR);
        }
        if(EmptyStockMap.get(goodsId)){
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        // 预减库存
        // Long stock = valueOperations.decrement("seckillGoods:" + goodsId);
        Long stock = (Long) redisTemplate.execute(script, Collections.singletonList("seckillGoods:" + goodsId), Collections.EMPTY_LIST);
        if(stock < 0){
            EmptyStockMap.put(goodsId, true);
            valueOperations.increment("seckillGoods:" + goodsId);
            return RespBean.error(RespBeanEnum.EMPTY_STOCK);
        }
        SeckillMessage seckillMessage = new SeckillMessage(user, goodsId);
        // 使用RabbitMq可以达到流量削峰的效果
        mqSender.sendSeckillMessage(JsonUtil.object2JsonStr(seckillMessage));

        return RespBean.success(0);
    }

    @RequestMapping(value = "/captcha", method = RequestMethod.GET)
    public void verifyCode(User user, Long goodsId, HttpServletResponse response){
        if(user == null || goodsId < 0){
            throw new GlobalException(RespBeanEnum.REQUEST_ILLEGAL);
        }
        // 设置请求头为输出图片的类型
        response.setContentType("image/jpg");
        response.setHeader("Pargam", "No-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        // 生成验证码，将结果放入redis
        ArithmeticCaptcha captcha = new ArithmeticCaptcha(130, 32, 3);
        redisTemplate.opsForValue().set("captcha:" + user.getId() + ":" + goodsId, captcha.text(), 300, TimeUnit.SECONDS);
        try{
            captcha.out(response.getOutputStream());
        } catch(IOException e){
            // e.printStackTrace();
            log.error("验证码生成失败", e.getMessage());
        }
    }
}
