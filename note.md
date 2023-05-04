
[视频地址](https://www.bilibili.com/video/BV1sf4y1L7KE)

> 注意要采用视频中的框架版本，少走弯路

创建数据表：
用户表：
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

[redis安装](https://blog.csdn.net/web18484626332/article/details/126540454)

```bash
# 进入容器
docker exec -it redis bash
# 启动 redis 客户端
redis-cli

redis 127.0.0.1:6379> PING
PONG

```

redis docker 安装：[1](https://cloud.tencent.com/developer/article/1670205),[2](https://www.jianshu.com/p/f62277cf5d0f)

```sql
create table `t_goods`(
	`id` BIGINT(20) not null AUTO_INCREMENT COMMENT '商品id',
	`goods_name` VARCHAR(16) DEFAULT NULL COMMENT '商品名称',
	`goods_title` VARCHAR(64) DEFAULT NULL COMMENT '商品标题',
	`goods_img` VARCHAR(64) DEFAULT NULL COMMENT '商品图片',
	`goods_detail` LONGTEXT  COMMENT '商品描述',
	`goods_price` DECIMAL(10, 2) DEFAULT '0.00' COMMENT '商品价格',
	`goods_stock` INT(11) DEFAULT '0' COMMENT '商品库存,-1表示没有限制',
	PRIMARY KEY(`id`)
)ENGINE = INNODB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `t_order` (
	`id` BIGINT(20) NOT NULL  AUTO_INCREMENT COMMENT '订单ID',
	`user_id` BIGINT(20) DEFAULT NULL COMMENT '用户ID',
	`goods_id` BIGINT(20) DEFAULT NULL COMMENT '商品ID',
	`delivery_addr_id` BIGINT(20) DEFAULT NULL  COMMENT '收获地址ID',
	`goods_name` VARCHAR(16) DEFAULT NULL  COMMENT '商品名字',
	`goods_count` INT(20) DEFAULT '0'  COMMENT '商品数量',
	`goods_price` DECIMAL(10,2) DEFAULT '0.00'  COMMENT '商品价格',
	`order_channel` TINYINT(4) DEFAULT '0'  COMMENT '1 pc,2 android, 3 ios',
	`status` TINYINT(4) DEFAULT '0'  COMMENT '订单状态，0新建未支付，1已支付，2已发货，3已收货，4已退货，5已完成',
	`create_date` datetime DEFAULT NULL  COMMENT '订单创建时间',
	`pay_date` datetime DEFAULT NULL  COMMENT '支付时间',
	PRIMARY KEY(`id`)
)ENGINE = INNODB AUTO_INCREMENT=12 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `t_seckill_goods`(
      `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀商品ID',
      `goods_id` BIGINT(20) NOT NULL COMMENT '商品ID',
      `seckill_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀家',
      `stock_count` INT(10) NOT NULL  COMMENT '库存数量',
      `start_date` datetime NOT NULL  COMMENT '秒杀开始时间',
      `end_date` datetime NOT NULL COMMENT '秒杀结束时间',
      PRIMARY KEY(`id`)
)ENGINE = INNODB AUTO_INCREMENT=3 DEFAULT CHARSET = utf8mb4;

CREATE TABLE `t_seckill_order` (
       `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '秒杀订单ID',
       `user_id` BIGINT(20) NOT NULL  COMMENT '用户ID',
       `order_id` BIGINT(20) NOT NULL  COMMENT '订单ID',
       `goods_id` BIGINT(20) NOT NULL  COMMENT '商品ID',
        PRIMARY KEY(`id`)
)ENGINE = INNODB AUTO_INCREMENT=3 DEFAULT CHARSET = utf8mb4;
```

```sql
INSERT INTO `t_goods` VALUES(1, 'IPHONE 12' , 'IPHONE12 64GB', '/img/iphone12.jpg', 'IPHONE12 64GB', '5299.00', 100),
                            (2, 'IPHONE12 PRO', 'IPHONE12 PRO 128GB', '/img/iphone12pro.jpg', 'IPHONE12 PRO 128GB', '9299.00', 100)

INSERT INTO `t_seckill_goods` VALUES(1, 1, '629', 10, '2020-11-01 08:00:00', '2020-11-01 09:00:00'),
                                  (2, 2, '929', 10, '2020-11-01 08:00:00', '2020-11-01 09:00:00')

```


linux安装mysql: [1](https://www.runoob.com/docker/docker-install-mysql.html),[2](https://www.cnblogs.com/sablier/p/11605606.html)

```bash
#进入容器
docker exec -it wjyMysql bash

#登录mysql
mysql -u root -p

```

```mysql
# ALTER USER 'root'@'localhost' IDENTIFIED BY 'Lzslov123!';

#添加远程登录用户
CREATE USER 'xxxx'@'%' IDENTIFIED WITH mysql_native_password BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'xxxx'@'%';

exit;
```


p30-p36不实践了，没时间了

配置自动注释：[《IDEA 方法注释动态添加param和return》](https://www.bilibili.com/read/cv22276744)


# Q & A

| 报错                                                                                   | 解决                                                                                                                             |
|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------|
| 项目运行报错`Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required`            | [1](https://cloud.tencent.com/developer/article/2177573),[2](https://github.com/baomidou/mybatis-plus/pull/4870)               |
| maven build报错`Cannot resolve plugin org.apache.maven.plugins:maven-clean-plugin3.0.0` | [1](https://blog.csdn.net/m0_67392126/article/details/124165634),[2](https://blog.csdn.net/liujucai/article/details/102450806) |
| Redis安装报错：`You need tcl 8.5 or newer in order to run the Redis test`                  | [1](https://blog.csdn.net/zhangshu123321/article/details/51440106)                                                             |


rabbitmq安装参考[《docker安装RabbitMq》](https://juejin.cn/post/6844903970545090574)

```bash
docker pull docker.io/rabbitmq:3.10-management

docker run --name wjyRabbitmq -d -p 15672:15672 -p 5672:5672 9347c9953e5a
```