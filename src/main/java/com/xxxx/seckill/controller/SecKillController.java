package com.xxxx.seckill.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @RequestMapping(value = "/doSeckill", method = RequestMethod.POST)
    @ResponseBody
    public RespBean doSecKill(Model model, User user, Long goodsId){
        if(user == null){
            return RespBean.error(RespBeanEnum.SESSION_ERROR);
        }
        ValueOperations valueOperations = redisTemplate.opsForValue();
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
}
