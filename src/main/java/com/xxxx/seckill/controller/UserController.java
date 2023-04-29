package com.xxxx.seckill.controller;


import com.xxxx.seckill.pojo.User;
import com.xxxx.seckill.vo.RespBean;
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
}
