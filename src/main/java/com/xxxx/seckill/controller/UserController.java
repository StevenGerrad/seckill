package com.xxxx.seckill.controller;


import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.rabbitmq.MQSender;
import com.xxxx.seckill.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author zhoubin
 * @since 2023-03-01
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private MQSender mqSender;

    /**
     * @description 用户信息(测试)
     * @param user
     * defaultParamDescription
     * @return RespBean
     * @author Administrator
     * @date 2023/4/22 15:45
     */
    @RequestMapping("/info")
    @ResponseBody
    public RespBean info(User user) {
        return RespBean.success(user);
    }


    /**
     * @description 测试发送rabbitmq消息
     * @author wangjunyou
     * @date 2023/5/4 9:55
     */
    // @RequestMapping("/mq")
    // @ResponseBody
    // public void mq(){
    //     mqSender.send("Hello");
    // }
    //
    // @RequestMapping("/mq/fanout")
    // @ResponseBody
    // public void mq01(){
    //     mqSender.send("Hello");
    // }
    //
    // @RequestMapping("/mq/direct01")
    // @ResponseBody
    // public void mq02(){
    //     mqSender.send01("Hello, Red");
    // }
    //
    // @RequestMapping("/mq/direct02")
    // @ResponseBody
    // public void mq03(){
    //     mqSender.send02("Hello, Green");
    // }
    //
    // @RequestMapping("/mq/topic01")
    // @ResponseBody
    // public void mq04(){
    //     mqSender.send03("Hello, Red");
    // }
    //
    // @RequestMapping("/mq/topic02")
    // @ResponseBody
    // public void mq05(){
    //     mqSender.send04("Hello, Green");
    // }
    //
    // @RequestMapping("/mq/header01")
    // @ResponseBody
    // public void mq06(){
    //     mqSender.send05("Hello, Header01");
    // }
    //
    // @RequestMapping("/mq/header02")
    // @ResponseBody
    // public void mq07(){
    //     mqSender.send06("Hello, Header02");
    // }
}
