
[视频地址](https://www.bilibili.com/video/BV1sf4y1L7KE)

> 注意要采用视频中的框架版本，少走弯路

创建数据表：
```sql
CREATE TABLE t_user(
   `id` BIGINT (20) NOT NULL COMMENT '用户ID，手机号码',
   `nickname` VARCHAR(255) NOT NULL,
   `password` VARCHAR(32) DEFAULT NULL COMMENT 'MD5(MD5(pass明文+固定salt)+salt)' ,
   `salt` VARCHAR(10) DEFAULT NULL,
   `head` VARCHAR(128) DEFAULT NULL COMMENT '头像',
   `register_date` datetime DEFAULT NULL COMMENT '注册时间',
   `last_login_date` datetime DEFAULT NULL COMMENT '最后一次登录时间"',
   `login_count` int(11) DEFAULT '0' COMMENT '登录次数',
   PRIMARY KEY(`id`)
)
```

[代码生成器](https://baomidou.com/pages/d357af/)



# Q & A

| 报错                                                                                    | 解决                                                                                                                             |
|---------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| 项目运行报错`Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`               | [1](https://cloud.tencent.com/developer/article/2177573),[2](https://github.com/baomidou/mybatis-plus/pull/4870)               |
| maven build报错`Cannot resolve plugin org.apache.maven.plugins:maven-clean-plugin3.0.0` | [1](https://blog.csdn.net/m0_67392126/article/details/124165634),[2](https://blog.csdn.net/liujucai/article/details/102450806) |

