> [乐字节秒杀系统](https://www.bilibili.com/video/BV1sf4y1L7KE)。文档参考课程文档与秦哥文档等。自实现仓库[StevenGerrad/seckill](https://github.com/StevenGerrad/seckill)。注意看仓库的markdown笔记，少走弯路。

# 跟进

## 页面优化
> 使用redis减少对数据库的访问

- 缓存
   - 页面缓存：使用redis缓存页面（设置超时时间60s）
   - 对象缓存：先修改数据库再删除redis
- 页面静态化（部分页面用服务器进行传输，只需要传输对象）
   - 商品详情页面静态化：用ajax拆分，前端即一个静态页面，不再通过模板引擎`model`去传输数据、进行页面跳转，而是直接在前端进行页面跳转，并通过ajax去接受数据、请求接口。实际上时使用了浏览器的缓存（刷新时浏览器的http码是304）
   - 秒杀静态化：主义需要配置`spring.web.resources`
   - 订单静态化
- **解决库存超卖**：
   - 使用`UpdateWrapper`语句，加入事务。
   - 在秒杀订单表前加唯一索引字段`seckill_uid_gid`，解决同一个用户同时秒杀多件商品的问题。
   - 改为用redis判断是否重复抢购
- QPS(1000 * 3)：从30升到556
## 服务优化（接口优化）
> 但仍需要频繁的与redis交互，这一节通过内存标记减少redis的交互。这一节优化下单操作，先用redis判断是狗有订单，进一步为减少数据库压力，使用消息队列，请求进入队列缓存，异步下单。（进一步甚至可以进行数据库集群，但本课程不涉及）


- RabbitMq使用
   - Exchange有四种模式
      - direct
         - 所有发送到Direct Exchange的消息被转发到RouteKey中指定的Queue 
         - 注意：Direct模式可以使用RabbitMQ自带的Exchange：default Exchange,所以不需要将 Exchange进行任何绑定(binding)操作，消息传递时，RouteKey必须完全匹配才会被队列接收，否则该消息会被抛弃。 
         - 重点：routing key与队列queues 的key保持一致，即可以路由到对应的queue中。  
      - topic
         - 所有发送到Topic Exchange的消息被转发到所有管线RouteKey中指定Topic的Queue上 Exchange将RouteKey和某Topic进行模糊匹配,此时队列需要绑定一个Topic
         - 对于routing key匹配模式定义规则举例如下: 
            - routing key为一个句点号 . 分隔的字符串（我们将被句点号 . 分隔开的每一段独立的字符串称为 一个单词），如“stock.usd.nyse”、“nyse.vmw”、“quick.orange.rabbit” 
            - routing key中可以存在两种特殊字符 * 与 # ，用于做模糊匹配，其中 * 用于匹配一个单词， # 用 于匹配多个单词（可以是零个）  
      - headers（不常用）
         - 不依赖routingkey，使用发送消息时basicProperties对象中的headers匹配队列 
         - headers是一个键值对类型，键值对的值可以是任何类型 
         - 在队列绑定交换机时用x-match来指定，all代表定义的多个键值对都要满足，any则代表只要满足 一个可以了  
      - fanout
         - 不处理路由键，只需要简单的将队里绑定到交换机上；
         - 发送到交换机的消息都会被转发到与该交换机绑定的所有队列上；
         - Fanout交换机转发消息是最快的；
- 接口优化
   - redis预减库存：`SeckillController`实现`InitializingBean`，项目初始时就加载库存到redis，秒杀时先在redis中进行预减库存。成功的话使用RabbitMQ进行下单。同时用内存标记（Map）标记redis库存是否用完，进一步减少对redis的访问。
   - RabbitMQ秒杀
   - 客户端轮询秒杀结果：由于采用了rabbitmq，所以要轮询。
   - QPS(1000 * 3)：从556升到1048
   - 【小实验】Redis实现分布式锁
      1. 简单实现：不存在kv键值对创建键值对，相当于获取成功，进行任务。任务结束后删除键值对。问题：如果代码有问题，就阻塞住了，锁永远删不掉，后面的线程也会卡住。
      2. 给锁添加一个过期时间，防止应用在运行过程中抛出异常导致锁无法正常释放。问题：这里的操作时间过长的话，自己的锁已经过期删掉了，这时候再删掉的是其它线程的锁。——不存v而是自己的随机值，但是这样删除锁的操作不是原子性的。
      3. 删除锁的系列操作使用lua脚本
         - Lua脚本优势： 
            - 使用方便，Redis内置了对Lua脚本的支持
            - Lua脚本可以在Rdis服务端原子的执行多个Redis命令
            - 由于网络在很大程度上会影响到Redis性能，使用Lua脚本可以让多个命令一次执行，可以有 效解决网络给Redis带来的性能问题
         - 使用Lua脚本思路： 
            - 提前在Redis服务端写好Lua脚本，然后在java客户端去调用脚本
            - 可以在java客户端写Lua脚本，写好之后，去执行。需要执行时，每次将脚本发送到Redis上 去执行
   - 优化Redis操作库存
      - 将redis预减库存操作扩展为lua脚本实现。

## 安全优化

- 秒杀接口地址隐藏：
   - 实现：
      - 秒杀开始前先将接口地址隐藏起来。秒杀后实际上是先获取了个人化的秒杀地址在进行秒杀
      - 真正的接口秒杀地址每个人是不一样的
   - 效果：
      - 可以防止脚本刷使得秒杀刚开始压力过大
      - （脚本仍能进行拼接）
- 图形验证码
   - 防止脚本刷
   - 进一步负载均衡
- 接口限流
   - 背景：
      - 限流方法一般有：计数器、漏斗、令牌桶
   - 简单接口限流
   - 通用接口限流：使用拦截器

## 主流秒杀方案分析

秒杀前根据预约数能够预估到秒杀人数，比如有500万，其中有200万给token（保存到用户浏览器本地），其它300万看单机动画（抢不到......）。同时对于持有token的用户要把重复请求过滤掉。
而对于脚本，在秒杀开始后仍会用程序频繁提交请求，就需要进行网关处理（eg.微服务中zoo/gateway/nginx/nginx+lua脚本），进行限流：黑名单；利用redis处理重复请求（甚至分片、二级缓存、在gbm内存里统计计数）；没有token的请求。比如只有20万商品，要在网关处终结多余请求。可以使用令牌桶。比如有100万请求，有20个网关，每个网关承担5万请求，每个tomcat在不做复杂计算时可承担1-2k的QPS，那么每个网关可以开1.5万个令牌，在1-3s放完，其他的请求在网关处就失败。幸运儿可以进入下单操作。
下单请求到达后迅速拼接好：订单对象/订单实体对象/订单商品对象等。把订单下到redis中（考虑redis的压力也可以做分片）——redis快；很多用户抢到后会频繁查询是否成功。同时就要用MQ进行流量削峰，后端慢慢消费。入库的话可以从MQ中慢慢消费，再做操作。
由于前面有多个网关，在分布式环境下可能会有超卖，一般需要有分布式锁。比如relation——redis的封装好的分布式锁方案（针对商品id加分布式锁）。但对同一个商品id进行20万操作时间也极长，只能对分布式锁再进行分片（如一个锁只放500个），这样会对redis压力比较大，进而需要对redis做集群。但这种方案比较复杂，难以控制，比如难以控制不同key的余量的消耗。
也可以不采用这种方案，而直接在服务器实例里写好商品数量，直接在内存里判断，不需要再去做redis了，也不用通信。比如20个订单的实例，20万个商品。用到微服务的话肯定要用配置中心如阿波罗、necos、console等，可以通过配置中心去下发每个实例的商品数量，且可以动态控制。可以在抢购开始的时候下发到每个服务1万个商品数量。服务的处理速度和请求不均匀可能会导致有些实例早就卖完了，别的还有大量剩余（比你晚的可能反而能买到，“这个没办法”）。而抢购的过程中服务挂掉了怎么办？——管不了，可以后续再统计再放出来。

## 总结

- 项目框架搭建
   1. SpringBoot环境搭建
   2. 集成Thymeleaf，RespBean
   3. MyBatis
- 分布式会话
   1. 用户登录
      1. 设计数据库
      2. 明文密码二次MD5加密
      3. 参数校验+全局异常处理
   2. 共享session
      1. SpringSession
      2. Redis
- 功能开发
   1. 商品列表
   2. 商品详情
   3. 秒杀
   4. 订单详情
- 系统压测
   1. JMeter
   2. 自定义变量去模拟多用户
   3. JMeter命令行使用
   4. 正式压测
      1. 商品列表
      2. 秒杀
- 页面优化
   1. 页面缓存+URL缓存+对象缓存
   2. 页面静态化，前后端分离
   3. 静态资源优化
   4. CDN优化
- 接口优化
   1. Redis预减库存减少数据库的访问
   2. 内存标记减少Redis的访问
   3. RabbitMQ异步下单
      1. SpringBoot整合RabbitMQ
      2. 交换机
- 安全优化
   1. 秒杀接口地址隐藏
   2. 算术验证码
   3. 接口防刷
- 主流的秒杀方案

在评论区找分析的时候找到的别人的比较详细的记录：[《B站秒杀项目》](https://goinggoinggoing.github.io/2023/03/20/B%E7%AB%99%E7%A7%92%E6%9D%80%E9%A1%B9%E7%9B%AE/)

# 分析
> 本来想找找，但是暂时没找到公开的技术细节笔记，一般只有业务逻辑笔记，只能自己攒了

## 业务逻辑
> 这部分是得益于秦哥的文档

1. 数据库池的作用
2. 两次MD5加密的作用 
   1. 第一次用户在前端输入明文密码传到后端之前进行一次md5加密（PASS = MD5（明文+固定salt）） 
      1. 防止数据传输过程中被捕获获取用户的明文密码
   2. 第二次在后端接收到前端加密后的密码再进行一次md5加密，最后再存到数据库里（PASS = MD5（PASS1+随机salt）） 
      1. 第二次是防止黑客脱裤，如果只进行一次加密，那么会存在黑客根据md5密码和盐进行逆向得到密码明文
3. nginx做负载均衡可能会出现一些问题： 
   1. 分布式session：请求通过nginx进行分发，可能会把请求分发到tomcat1上，也可能分发到tomcat2上。如果是常规的在服务器上创建对cookie进行判断，那么最初用户的请求到tomcat1上时创建了session，后续不需要登陆，但当用户请求到tomcat2上时又会为要求用户登陆并为用户新建session，不合常理。 
      1. 解决办法： 
         1. session复制： 
            1. 优点：无需修改代码，只需要修改Tomcat配置
            2. 缺点：session同步传输占用内网带宽、多台Tomcat同步性能指数级下降、session占用内存，无法有效水平扩展
         2. 前端存储： 
            1. 优点：不占用服务端内存
            2. 缺点：存在安全风险、数据大小受cookie限制、占用外网带宽
         3. Session粘滞： 
            1. 优点：无需修改代码、服务端可以水平扩展
            2. 缺点：增加新机器，会重新Hash导致重新登陆、应用重启需要重新登陆
         4. 后端集中存储 
            1. 优点：安全、容易水平扩展
            2. 缺点：会增加复杂度，需要修改代码
4. 项目里的Session实现： 
   1. 使用redis+SpringSession实现：只需要在maven里面导入依赖（spring-redis-session），然后在properties配置文件里面配置一下就可以自动实现：将session存入到redis里
   2. 不使用spring-session，只使用spring-redis实现用户session：通过实现一个模版类，实现User类的序列化，便于存入redis中
5. 表： 
   1. 商品表：商品ID、商品名称、商品标题、图片、介绍、价格、库存
   2. 商品秒杀表：主键ID、商品ID、秒杀价格、库存数量、秒杀开始时间、秒杀结束时间
   3. 订单表：主键ID、用户ID、商品ID、商品名称、商品价格、收货地址ID、商品数量、订单来源、订单状态、创建时间、支付时间
   4. 秒杀订单表：主键ID、用户ID、订单ID、商品ID
   5. 用户表：用户ID、昵称、密码、盐、头像、注册时间、上次登录时间、登录次数
6. 秒杀判断 
   1. （传入参数健壮性）
   2. 校验个人秒杀地址与userId是否对齐
   3. redis判断是否重复抢购
   4. redis预减库存
   5. RabbitMq削峰进行业务处理（后续还会再次判断库存以及是否重复抢购）

## 技术逻辑
> 以问题驱动。参考[Java 全栈知识体系](https://pdai.tech)、[沉默王二-《面渣逆袭（Spring面试题八股文）必看👍》](https://tobebetterjavaer.com/sidebar/sanfene/spring.html)

### 宏观
> 参考


Spring

- Spring怎么理解？
   - Spring 是一个轻量级、非入侵式的控制反转 (IoC) 和面向切面 (AOP) 的框架。
- Spring的核心功能？
   1. IOC 和 DI 的支持
      - Spring 的核心就是一个大的工厂容器，可以维护所有对象的创建和依赖关系，Spring 工厂用于生成 Bean，并且管理 Bean 的生命周期，实现**高内聚低耦合**的设计理念。
   2. AOP 编程的支持
      - Spring 提供了**面向切面编程**，可以方便的实现对程序进行权限拦截、运行监控等切面功能。
   3. 声明式事务的支持
      - 支持通过配置就来完成对事务的管理，而不需要通过硬编码的方式，以前重复的一些事务提交、回滚的 JDBC 代码，都可以不用自己写了。
   4. 快捷测试的支持
      - Spring 对 Junit 提供支持，可以通过**注解**快捷地测试 Spring 程序。
   5. 快速集成功能
      - 方便集成各种优秀框架，Spring 不排斥各种优秀的开源框架，其内部提供了对各种优秀框架（如：Struts、Hibernate、MyBatis、Quartz 等）的直接支持。
   6. 复杂 API 模板封装
      - Spring 对 JavaEE 开发中非常难用的一些 API（JDBC、JavaMail、远程调用等）都提供了模板化的封装，这些封装 API 的提供使得应用难度大大降低。
- Spring有哪些模块？
   - Spring 框架是分模块存在，除了最核心的`Spring Core Container`是必要模块之外，其他模块都是可选，大约有 20 多个模块。
      1. Spring Core：Spring 核心，它是框架最基础的部分，提供 IOC 和依赖注入 DI 特性。
      2. Spring Context：**Spring 上下文容器，它是 BeanFactory 功能加强的一个子接口**。
      3. Spring Web：它提供 Web 应用开发的支持。【TODO：集成了Tomcat、Servlet？】
      4. Spring MVC：它针对 Web 应用中 MVC 思想的实现。
      5. Spring DAO：提供对 JDBC 抽象层，简化了 JDBC 编码，同时，编码更具有健壮性。【TODO：专门有DAO这么个模块吗？图里怎么没有呢？】
      6. Spring ORM：它支持用于流行的 ORM 框架的整合，比如：Spring + Hibernate、Spring + iBatis、Spring + JDO 的整合等。
      7. Spring AOP：即面向切面编程，它提供了与 AOP 联盟兼容的编程实现。
- Spring和别的框架（如Hibernate、MyBatis）有什么区别？
- 为什么要分controller、service（和serviceImpl）、mapper、pojo层：
   - [《Service 层和 Dao 层有必要为每个类都加上接口吗？》](https://zhuanlan.zhihu.com/p/436394551)
- Spring的设计模式？
   1. 工厂模式 : Spring 容器本质是一个大工厂，使用工厂模式通过 BeanFactory、ApplicationContext 创建 bean 对象。
   2. 代理模式 : Spring AOP 功能功能就是通过代理模式来实现的，分为动态代理和静态代理。
   3. 单例模式 : **Spring 中的 Bean 默认都是单例的**，这样有利于容器对 Bean 的管理。
   4. 模板模式 : Spring 中 JdbcTemplate、RestTemplate 等以 Template 结尾的对数据库、网络等等进行操作的模板类，就使用到了模板模式。
   5. 观察者模式: Spring 事件驱动模型就是观察者模式很经典的一个应用。
   6. 适配器模式 :Spring AOP 的增强或通知 (Advice) 使用到了适配器模式、Spring MVC 中也是用到了适配器模式适配 Controller。
   7. 策略模式：Spring 中有一个 Resource 接口，它的不同实现类，会根据不同的策略去访问资源。
- Spring的事务原理？
- Spring的生命周期？
- Spring中出现循环注入怎么解决？
- boot有哪些特性
- 如果没有Spring我们的开发会变成什么样子？
   - **Spring的一个最大的目的就是使JAVA EE开发更加容易**。同时，Spring之所以与Struts、Hibernate等单层框架不同，是因为Spring致力于提供一个以统一的、高效的方式构造整个应用，并且可以将单层框架以最佳的组合揉和在一起建立一个连贯的体系。可以说Spring是一个提供了更完善开发环境的一个框架，可以为POJO(Plain Ordinary Java Object)对象提供企业级的服务。
- Springboot自动配置
- 什么是MVC（尚硅谷SSM）：
   - MVC是一种软件架构的思想，将软件按照模型、视图、控制器来划分 
      - M：Model，模型层，指工程中的JavaBean，作用是处理数据。JavaBean分为两类：
         - 一类称为实体类Bean：专门存储业务数据的，如 Student、User 等
         - 一类称为业务处理 Bean：指 Service 或 Dao 对象，专门用于处理业务逻辑和数据访问。 
      - V：View，视图层，指工程中的html或jsp等页面，作用是与用户进行交互，展示数据 
      - C：Controller，控制层，指工程中的servlet，作用是接收请求和响应浏览器  
   - MVC的工作流程： 用户通过视图层发送请求到服务器，在服务器中请求被Controller接收，Controller 调用相应的Model层处理请求，处理完毕将结果返回到Controller，Controller再根据请求处理的结果 找到相应的View视图，渲染数据后最终响应给浏览器
- 什么是SpringMVC（尚硅谷SSM）：
   - SpringMVC是Spring的一个后续产品，是Spring的一个子项目
   - SpringMVC 是 Spring 为表述层开发提供的一整套完备的解决方案。在表述层框架历经 Strust、 WebWork、Strust2 等诸多产品的历代更迭之后，目前业界普遍选择了 SpringMVC 作为 Java EE 项目 表述层开发的**首选方案**。
   - 注：三层架构分为表述层（或表示层）、业务逻辑层、数据访问层，表述层表示前台页面和后台 servlet
- SpringMVC域对象共享数据
- SpringMVC机制，Servlet

IoC

- 定义：
   - 控制反转：IOC——Inversion of Control，指的是将对象的创建权交给 Spring 去创建。使用 Spring 之前，对象的创建都是由我们自己在代码中new创建。而使用 Spring 之后。对象的创建都是给了 Spring 框架。
   - DI（依赖注入）：指的是容器在实例化对象的时候把它依赖的类注入给它。有的说法 IOC 和 DI 是一回事，有的说法是 IOC 是思想，DI 是 IOC 的实现。
- 你有自己实现过简单的 Spring 吗？

![“你有自己实现过简单的 Spring 吗？”——mini版本Spring IOC](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683685849107-8cc0ce3e-0965-4f36-8ceb-cb6846a70b7e.png#averageHue=%23f4f4e2&clientId=u58a0abf6-6568-4&from=paste&height=238&id=u2becd91a&originHeight=299&originWidth=1061&originalType=url&ratio=1&rotation=0&showTitle=true&size=50673&status=done&style=none&taskId=u0a8aeaf0-c508-4f6c-a895-e5fa62cfd40&title=%E2%80%9C%E4%BD%A0%E6%9C%89%E8%87%AA%E5%B7%B1%E5%AE%9E%E7%8E%B0%E8%BF%87%E7%AE%80%E5%8D%95%E7%9A%84%20Spring%20%E5%90%97%EF%BC%9F%E2%80%9D%E2%80%94%E2%80%94mini%E7%89%88%E6%9C%ACSpring%20IOC&width=845 "“你有自己实现过简单的 Spring 吗？”——mini版本Spring IOC")

   - Bean 通过一个配置文件定义，把它解析成一个类型，编写`beans.properties`。eg. `userDao:cn.fighter3.bean.UserDao`
   - 编写`BeanDefinition.java`（只有两个字段）`String beanName; Class beanClass;`
   - 实现`ResourceLoader.java`：资源加载器，用来完成配置文件中配置的加载。
      1. 扫描`beans.properties`
      2. 利用反射机制初始化`BeanDefinition`
      3. 返回一个`Map<String, BeanDefinition>`
   - 实现`BeanRegister.java`对象注册器，这里用于单例 bean 的**缓存**（有一个private的`Map<String, Object>`），我们大幅简化，默认所有 bean 都是单例的。可以看到所谓单例注册，也很简单，不过是往 HashMap 里存对象。
   - 实现`BeanFactory.java`
      - 对象工厂，我们最**核心**的一个类，在它初始化的时候，创建了 bean 注册器，完成了资源的加载。
      - 获取 bean 的时候，先从单例缓存中取，如果没有取到，就创建并注册一个 bean：
         1. 具有`BeanRegister` 和 `Map<String, BeanDefinition>`（调用`ResourceLoader`加载得来）属性
         2. 每次获取Bean，先从尝试从beanRegister缓存中获取。
         3. 没有的话从Map中取`BeanDefinition`，并进行初始化后，存入beanRegister缓存。
- 说说 BeanFactory 和 ApplicantContext?
   - 可以这么形容，BeanFactory 是 Spring 的“心脏”，ApplicantContext 是完整的“身躯”。
      - BeanFactory（Bean 工厂）是 Spring 框架的基础设施，面向 Spring 本身。
      - ApplicantContext（应用上下文）建立在 BeanFactoty 基础上，面向使用 Spring 框架的开发者。
   - BeanFactory 接口
      - BeanFactory 是类的通用工厂，可以创建并管理各种类的对象。
      - Spring 为 BeanFactory 提供了很多种实现，最常用的是 XmlBeanFactory，但在 Spring 3.2 中已被废弃，建议使用 XmlBeanDefinitionReader、DefaultListableBeanFactory。

![Spring5 BeanFactory继承体系](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683688775433-f11ab523-7727-4804-ac38-bd064fa405e7.png#averageHue=%23faf9f8&clientId=u58a0abf6-6568-4&from=paste&id=u700ea90f&originHeight=816&originWidth=1227&originalType=url&ratio=1&rotation=0&showTitle=true&size=129954&status=done&style=none&taskId=ua770cbfb-bfeb-402f-a49d-1a9613de8b8&title=Spring5%20BeanFactory%E7%BB%A7%E6%89%BF%E4%BD%93%E7%B3%BB "Spring5 BeanFactory继承体系")

      - BeanFactory 接口位于类结构树的顶端，它最主要的方法就是 getBean(String var1)，这个方法从容器中返回特定名称的 Bean。
      - BeanFactory 的功能通过其它的接口得到了不断的扩展，比如 AbstractAutowireCapableBeanFactory 定义了将容器中的 Bean 按照某种规则（比如按名字匹配、按类型匹配等）进行自动装配的方法。
   - ApplicationContext 接口
      - ApplicationContext 由 BeanFactory 派生而来，提供了更多面向实际应用的功能。可以这么说，使用 BeanFactory 就是手动档，使用 ApplicationContext 就是自动档。

![Spring5 ApplicationContext部分体系类图](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683688851562-373f94bc-7543-4b1c-b59b-ea0b8b829e69.png#averageHue=%23fcfaf9&clientId=u58a0abf6-6568-4&from=paste&id=u8fa30180&originHeight=792&originWidth=1140&originalType=url&ratio=1&rotation=0&showTitle=true&size=104797&status=done&style=none&taskId=u2b503749-177e-4605-a909-d2f4816033b&title=Spring5%20ApplicationContext%E9%83%A8%E5%88%86%E4%BD%93%E7%B3%BB%E7%B1%BB%E5%9B%BE "Spring5 ApplicationContext部分体系类图")

      - ApplicationContext 继承了 HierachicalBeanFactory 和 ListableBeanFactory 接口，在此基础上，还通过其他的接口扩展了 BeanFactory 的功能，包括：
         - Bean instantiation/wiring
         - Bean 的实例化/串联
         - 自动的 BeanPostProcessor 注册
         - 自动的 BeanFactoryPostProcessor 注册
         - 方便的 MessageSource 访问（i18n）
         - ApplicationEvent 的发布与 BeanFactory 懒加载的方式不同，它是预加载，所以，每一个 bean 都在 ApplicationContext 启动之后实例化
- 你知道 Spring 容器启动阶段会干什么吗？
   - Spring 的 IOC 容器工作的过程，其实可以划分为两个阶段：容器启动阶段和Bean 实例化阶段。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683688947642-628b916e-2d28-4655-9c97-f9a8ec1263e1.png#averageHue=%23e6f1c5&clientId=u58a0abf6-6568-4&from=paste&id=uf71d8b8f&originHeight=368&originWidth=755&originalType=url&ratio=1&rotation=0&showTitle=false&size=40096&status=done&style=none&taskId=u8d4a5a99-60b1-4371-a6e6-cb85378b3e5&title=)

      - 其中容器启动阶段主要做的工作是加载和解析配置文件，保存到对应的 Bean 定义中。
      - 容器启动开始，首先会通过某种途径加载 Congiguration MetaData，在大部分情况下，容器需要依赖某些工具类（BeanDefinitionReader）对加载的 Congiguration MetaData 进行解析和分析，并将分析后的信息组为相应的 BeanDefinition。
      - 最后把这些保存了 Bean 定义必要信息的 BeanDefinition，注册到相应的 BeanDefinitionRegistry，这样容器启动就完成了。
- 能说一下 Spring Bean 生命周期吗？
   - Spring IOC 中 Bean 的生命周期大致分为四个阶段：实例化（Instantiation）、属性赋值（Populate）、初始化（Initialization）、销毁（Destruction）。
   - 【生命周期这个还是Java全栈写得好点】
   - eg.
      - 定义一个`PersonBean`类，实现`DisposableBean`,`InitializingBean`, `BeanFactoryAware`, `BeanNameAware`这 4 个接口，同时还有自定义的`init-method`和`destroy-method`。
      - 定义一个`MyBeanPostProcessor`实现`BeanPostProcessor`接口。
      - 配置文件，指定`init-method`和`destroy-method`属性
   - 关于源码，Bean 创建过程可以查看`AbstractBeanFactory#doGetBean`方法，在这个方法里可以看到 Bean 的实例化，赋值、初始化的过程，至于最终的销毁，可以看看`ConfigurableApplicationContext#close()`。
- Bean 定义和依赖定义有哪些方式？
   - 直接编码方式：我们一般接触不到直接编码的方式，但其实其它的方式最终都要通过直接编码来实现。
   - 配置文件方式：通过 xml、propreties 类型的配置文件，配置相应的依赖关系，Spring 读取配置文件，完成依赖关系的注入。
   - 注解方式：注解方式应该是我们用的最多的一种方式了，在相应的地方使用注解修饰，Spring 会扫描注解，完成依赖关系的注入。
- 有哪些依赖注入的方法？
   - 构造方法注入：通过调用类的构造方法，将接口实现类通过构造方法变量传入
   - 属性注入：通过 Setter 方法完成调用类所需依赖的注入
   - 工厂方法注入【TODO】
      - 静态工厂注入：静态工厂顾名思义，就是通过调用静态工厂的方法来获取自己需要的对象，为了让 Spring 管理所有对象，我们不能直接通过"工程类.静态方法()"来获取对象，而是依然通过 Spring 注入的形式获取
      - 非静态工厂注入：非静态工厂，也叫实例工厂，意思是工厂方法不是静态的，所以我们需要首先 new 一个工厂实例，再调用普通的实例方法。
- Spring 有哪些自动装配的方式？
   - 什么是自动装配？
      - Spring IOC 容器知道所有 Bean 的配置信息，此外，通过 Java 反射机制还可以获知实现类的结构信息，如构造方法的结构、属性等信息。掌握所有 Bean 的这些信息后，Spring IOC 容器就可以按照某种规则对容器中的 Bean 进行自动装配，而无须通过显式的方式进行依赖配置。
      - Spring 提供的这种方式，可以按照某些规则进行 Bean 的自动装配，`<bean>`元素提供了一个指定自动装配类型的属性：`autowire="<自动装配类型>"`
   - Spring 提供了哪几种自动装配类型？
      - byName：根据名称进行自动匹配，假设 Boss 有一个名为 car 的属性，如果容器中刚好有一个名为 car 的 bean，Spring 就会自动将其装配给 Boss 的 car 属性
      - byType：根据类型进行自动匹配，假设 Boss 有一个 Car 类型的属性，如果容器中刚好有一个 Car 类型的 Bean，Spring 就会自动将其装配给 Boss 这个属性
      - constructor：与 byType 类似， 只不过它是针对构造函数注入而言的。如果 Boss 有一个构造函数，构造函数包含一个 Car 类型的入参，如果容器中有一个 Car 类型的 Bean，则 Spring 将自动把这个 Bean 作为 Boss 构造函数的入参；如果容器中没有找到和构造函数入参匹配类型的 Bean，则 Spring 将抛出异常。
      - autodetect：根据 Bean 的自省机制决定采用 byType 还是 constructor 进行自动装配，如果 Bean 提供了默认的构造函数，则采用 byType，否则采用 constructor。
- Spring 中的 Bean 的作用域有哪些?——有5种
   - **singleton** : 在 Spring 容器仅存在一个 Bean 实例，Bean 以单实例的方式存在，是 Bean 默认的作用域。
   - **prototype **: 每次从容器重调用 Bean 时，都会返回一个新的实例。
   - （以下三个作用域于只在 Web 应用中适用：）
   - **request **: 每一次 HTTP 请求都会产生一个新的 Bean，该 Bean 仅在当前 HTTP Request 内有效。
   - **session **: 同一个 HTTP Session 共享一个 Bean，不同的 HTTP Session 使用不同的 Bean。
   - **globalSession**：同一个全局 Session 共享一个 Bean，只用于基于 Protlet 的 Web 应用，Spring5 中已经不存在了。
- Spring 中的单例 Bean 会存在线程安全问题吗？
   - 首先结论在这：Spring 中的**单例 Bean不是线程安全的**。
      - 因为单例 Bean，是全局只有一个 Bean，所有线程共享。如果说单例 Bean，是一个**无状态的**，也就是线程中的操作不会对 Bean 中的成员变量执行查询以外的操作，那么这个单例 Bean 是线程安全的。**比如 Spring mvc 的 Controller、Service、Dao 等**，这些 Bean 大多是无状态的，只关注于方法本身。
      - 假如这个 Bean 是有状态的，也就是会对 Bean 中的成员变量进行写操作，那么可能就存在线程安全的问题
   - 单例 Bean 线程安全问题怎么解决呢？
      1. 将 Bean 定义为多例：这样每一个线程请求过来都会创建一个新的 Bean，但是这样容器就不好管理 Bean，不能这么办。
      2. 在 Bean 对象中尽量避免定义可变的成员变量：削足适履了属于是，也不能这么干。
      3. 将 Bean 中的成员变量保存在 ThreadLocal 中 ⭐：我们知道 ThredLoca 能保证多线程下变量的隔离，可以在类中定义一个 ThreadLocal 成员变量，将需要的可变成员变量保存在 ThreadLocal 里，这是推荐的一种方式。
- 说说循环依赖?
   - Spring 循环依赖：简单说就是自己依赖自己，或者和别的 Bean 相互依赖。
   - **只有单例的 Bean 才存在循环依赖的情况，原型(Prototype)情况下，Spring 会直接抛出异常**。原因很简单，AB 循环依赖，A 实例化的时候，发现依赖 B，创建 B 实例，创建 B 的时候发现需要 A，创建 A1 实例……无限套娃，直接把系统干垮。
   - Spring 可以解决哪些情况的循环依赖？

![image.png](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683690783968-262ced54-5948-433a-965e-fc8c3eb9dd85.png#averageHue=%232f9b07&clientId=u58a0abf6-6568-4&from=paste&id=u8dffafd4&originHeight=517&originWidth=865&originalType=url&ratio=1&rotation=0&showTitle=false&size=82789&status=done&style=none&taskId=udacf9a90-3e41-4955-aa8e-08b422008d5&title=)

      - 第四种可以而第五种不可以的原因是 Spring 在创建 Bean 时默认会根据自然排序进行创建，所以 A 会先于 B 进行创建。
      - 所以简单总结，当循环依赖的实例都采用 setter 方法注入的时候，Spring 可以支持，都采用构造器注入的时候，不支持，构造器注入和 setter 注入同时存在的时候，看天。
- 那 Spring 怎么解决循环依赖的呢？
   - PS：其实正确答案是开发人员做好设计，别让 Bean 循环依赖，但是没办法，面试官不想听这个。
   - 我们都知道，单例 Bean 初始化完成，要经历三步：实例化 createBeanInstance，属性赋值 populateBean，初始化 initializeBean。**注入就发生在第二步，属性赋值**，结合这个过程，Spring 通过三级缓存解决了循环依赖：
      1. 一级缓存 : Map<String,Object> **singletonObjects**，单例池，用于保存实例化、属性赋值（注入）、初始化完成的 bean 实例
      2. 二级缓存 : Map<String,Object> **earlySingletonObjects**，早期曝光对象，用于保存实例化完成的 bean 实例
      3. 三级缓存 : Map<String,ObjectFactory<?>> **singletonFactories**，早期曝光对象工厂，用于保存 bean 创建工厂，以便于后面扩展有机会创建代理对象。
      - A 实例的初始化过程：
         1. 创建 A 实例，实例化的时候把 A 对象⼯⼚放⼊三级缓存，表示 A 开始实例化了，虽然我这个对象还不完整，但是先曝光出来让大家知道
         2. A 注⼊属性时，发现依赖 B，此时 B 还没有被创建出来，所以去实例化 B
         3. 同样，B 注⼊属性时发现依赖 A，它就会从缓存里找 A 对象。依次从⼀级到三级缓存查询 A，从三级缓存通过对象⼯⼚拿到 A，发现 A 虽然不太完善，但是存在，把 A 放⼊⼆级缓存，同时删除三级缓存中的 A，此时，B 已经实例化并且初始化完成，把 B 放入⼀级缓存。
         4. 接着 A 继续属性赋值，顺利从⼀级缓存拿到实例化且初始化完成的 B 对象，A 对象创建也完成，删除⼆级缓存中的 A，同时把 A 放⼊⼀级缓存
         5. 最后，⼀级缓存中保存着实例化、初始化都完成的 A、B 对象
      - 我们就知道为什么 Spring 能解决 setter 注入的循环依赖了，因为实例化和属性赋值是分开的，所以里面有操作的空间。如果都是**构造器注入的话，那么都得在实例化这一步完成注入**，所以自然是无法支持了。
   - 为什么要三级缓存？⼆级不⾏吗？
      - 不行，主要是为了**⽣成代理对象**。如果是没有代理的情况下，使用二级缓存解决循环依赖也是 OK 的。但是如果存在代理，三级没有问题，二级就不行了。
      - 因为三级缓存中放的是⽣成具体对象的匿名内部类，获取 Object 的时候，它可以⽣成代理对象，也可以返回普通对象。使⽤三级缓存主要是为了保证不管什么时候使⽤的都是⼀个对象。
      - 假设只有⼆级缓存的情况，往⼆级缓存中放的显示⼀个普通的 Bean 对象，Bean 初始化过程中，通过 BeanPostProcessor 去⽣成代理对象之后，覆盖掉⼆级缓存中的普通 Bean 对象，那么可能就导致取到的 Bean 对象不一致了。【为什么会覆盖掉？】
- @Autowired 的实现原理？
   - 实现@Autowired 的关键是：**AutowiredAnnotationBeanPostProcessor**
   - 在 Bean 的初始化阶段，会通过 Bean 后置处理器来进行一些前置和后置的处理。
   - 实现@Autowired 的功能，也是通过后置处理器来完成的。这个后置处理器就是 AutowiredAnnotationBeanPostProcessor。
      - Spring 在创建 bean 的过程中，最终会调用到 doCreateBean()方法，在 doCreateBean()方法中会调用 populateBean()方法，来为 bean 进行属性填充，完成自动装配等工作。
      - 在 populateBean()方法中一共调用了两次后置处理器，第一次是为了判断是否需要属性填充，如果不需要进行属性填充，那么就会直接进行 return，如果需要进行属性填充，那么方法就会继续向下执行，后面会进行第二次后置处理器的调用，这个时候，就会调用到 AutowiredAnnotationBeanPostProcessor 的 postProcessPropertyValues()方法，在该方法中就会进行@Autowired 注解的解析，然后实现自动装配。
      - postProcessorPropertyValues()方法的源码如下，在该方法中，会先调用 findAutowiringMetadata()方法解析出 bean 中带有@Autowired 注解、@Inject 和@Value 注解的属性和方法。然后调用 metadata.inject()方法，进行属性填充。
- IoC怎么拿到一个Bean

AOP

- 说说什么是 AOP？
   - AOP：面向切面编程。简单说，就是把一些业务逻辑中的相同的代码抽取到一个独立的模块中，让业务逻辑更加清爽。
   - 业务逻辑代码中没有参和通用逻辑的代码，业务模块更简洁，只包含核心业务代码。实现了业务逻辑和通用逻辑的代码分离，便于维护和升级，降低了业务逻辑和通用逻辑的耦合性。
   - AOP 可以将遍布应用各处的功能分离出来形成可重用的组件。在编译期间、装载期间或运行期间实现在不修改源代码的情况下给程序动态添加功能。从而实现对业务逻辑的隔离，提高代码的模块化能力。
   - AOP 的核心其实就是动态代理，如果是实现了接口的话就会使用 JDK 动态代理，否则使用 CGLIB 代理，主要应用于处理一些具有横切性质的系统级服务，如日志收集、事务管理、安全检查、缓存、对象池管理等。
   - AOP 有哪些核心概念？
      - 切面（Aspect）：类是对物体特征的抽象，切面就是对横切关注点的抽象
      - 连接点（Joinpoint）：被拦截到的点，因为 Spring 只支持方法类型的连接点，所以在 Spring 中连接点指的就是被拦截到的方法，实际上连接点还可以是字段或者构造器
      - 切点（Pointcut）：对连接点进行拦截的定位
      - 通知（Advice）：所谓通知指的就是指拦截到连接点之后要执行的代码，也可以称作增强
      - 目标对象 （Target）：代理的目标对象
      - 织入（Weabing）：织入是将增强添加到目标类的具体连接点上的过程。
         - 编译期织入：切面在目标类编译时被织入
         - 类加载期织入：切面在目标类加载到 JVM 时被织入。需要特殊的类加载器，它可以在目标类被引入应用之前增强该目标类的字节码。
         - 运行期织入：切面在应用运行的某个时刻被织入。一般情况下，在织入切面时，AOP 容器会为目标对象动态地创建一个代理对象。SpringAOP 就是以这种方式织入切面。Spring 采用运行期织入，而 AspectJ 采用编译期织入和类加载器织入。
      - 引介（introduction）：引介是一种特殊的增强，可以动态地为类添加一些属性和方法
   - AOP 有哪些环绕方式？
      - 前置通知 (@Before)
      - 返回通知 (@AfterReturning)
      - 异常通知 (@AfterThrowing)
      - 后置通知 (@After)
      - 环绕通知 (@Around)
- 说说你平时有用到 AOP 吗？
   - 这里给出一个小例子，SpringBoot 项目中，利用 AOP 打印接口的入参和出参日志，以及执行时间，还是比较快捷的。
   - 引入依赖：（在maven配置文件pom.xml中）引入 AOP 依赖。
   - 自定义注解：自定义一个注解作为切点
   - 配置 AOP 切面：
      - @Aspect：标识切面
      - @Pointcut：设置切点，这里以自定义注解为切点，定义切点有很多其它种方式，自定义注解是比较常用的一种。
      - @Before：在切点之前织入，打印了一些入参信息
      - @Around：环绕切点，打印返回参数和接口执行时间
   - 使用：只需要在接口上加上自定义注解
- 说说 JDK 动态代理和 CGLIB 代理 ？
   - Spring 的 AOP 是通过动态代理来实现的，动态代理主要有两种方式 JDK 动态代理和 Cglib 动态代理，这两种动态代理的使用和原理有些不同。
   - JDK 动态代理
      1. Interface：对于 JDK 动态代理，目标类需要实现一个 Interface。
      2. InvocationHandler：InvocationHandler 是一个接口，可以通过实现这个接口，定义横切逻辑，再通过反射机制（invoke）调用目标类的代码，在次过程，可能包装逻辑，对目标方法进行前置后置处理。
      3. Proxy：Proxy 利用 InvocationHandler 动态创建一个符合目标类实现的接口的实例，生成目标类的代理对象。
   - CgLib 动态代理
      1. 使用 JDK 创建代理有一大限制，它只能为接口创建代理实例，而 CgLib 动态代理就没有这个限制。
      2. CgLib 动态代理是使用字节码处理框架 ASM，其原理是通过字节码技术为一个类创建子类，并在子类中采用方法拦截的技术拦截所有父类方法的调用，顺势织入横切逻辑。
      3. CgLib 创建的动态代理对象性能比 JDK 创建的动态代理对象的性能高不少，但是 CGLib 在创建代理对象时所花费的时间却比 JDK 多得多，所以对于单例的对象，因为无需频繁创建对象，用 CGLib 合适，反之，使用 JDK 方式要更为合适一些。同时，由于 CGLib 由于是采用动态创建子类的方法，对于 final 方法，无法进行代理。
- 说说 Spring AOP 和 AspectJ AOP 区别?
   - Spring AOP
      - Spring AOP 属于运行时增强，主要具有如下特点：
      1. 基于动态代理来实现，默认如果使用接口的，用 JDK 提供的动态代理实现，如果是方法则使用 CGLIB 实现
      2. Spring AOP 需要依赖 IOC 容器来管理，并且只能作用于 Spring 容器，使用纯 Java 代码实现
      3. 在性能上，由于 Spring AOP 是基于**动态代理**来实现的，在容器启动时需要生成代理实例，在方法调用上也会增加栈的深度，使得 Spring AOP 的性能不如 AspectJ 的那么好。
      4. Spring AOP 致力于解决企业级开发中最普遍的 AOP(方法织入)。
   - AspectJ
      - AspectJ 是一个易用的功能强大的 AOP 框架，属于编译时增强， 可以单独使用，也可以整合到其它框架中，是 AOP 编程的完全解决方案。AspectJ 需要用到单独的编译器 ajc。
      - AspectJ 属于**静态织入**，通过修改代码来实现，在实际运行之前就完成了织入，所以说它生成的类是没有额外运行时开销的，一般有如下几个织入的时机：
      1. 编译期织入（Compile-time weaving）：如类 A 使用 AspectJ 添加了一个属性，类 B 引用了它，这个场景就需要编译期的时候就进行织入，否则没法编译类 B。
      2. 编译后织入（Post-compile weaving）：也就是已经生成了 .class 文件，或已经打成 jar 包了，这种情况我们需要增强处理的话，就要用到编译后织入。
      3. 类加载后织入（Load-time weaving）：指的是在加载类的时候进行织入，要实现这个时期的织入，有几种常见的方法

![Spring AOP和AspectJ对比](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683698213103-63a3dcc0-3ffe-42ef-982e-d598f2c41f3f.png#averageHue=%232ac8d1&clientId=u58a0abf6-6568-4&from=paste&id=u947f43cf&originHeight=565&originWidth=930&originalType=url&ratio=1&rotation=0&showTitle=true&size=123020&status=done&style=none&taskId=u330fe4ba-a9e0-40e3-935d-4c6a6a62575&title=Spring%20AOP%E5%92%8CAspectJ%E5%AF%B9%E6%AF%94 "Spring AOP和AspectJ对比")

- Spring AOP在哪个环节织入？
- Spring AOP相关技术：
   - 动态代理（InvocationHandler）：JDK原生的实现方式，需要被代理的目标类必须实现接口。因为这个技术要求代理对象和目标对象实现同样的接口（兄弟两个拜把子模式）。 
   - cglib：通过继承被代理的目标类（认干爹模式）实现代理，所以不需要目标类实现接口。 
   - AspectJ：本质上是静态代理，将代理逻辑“织入”被代理的目标类编译得到的字节码文件，所以最终效果是动态的。weaver就是织入器。Spring只是借用了AspectJ中的注解。

![image.png](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683510989379-d71ad000-28bd-42e0-bc73-1baccba473f0.png#averageHue=%23ededed&clientId=u0fa7ff71-666a-4&from=paste&height=376&id=u76c29dca&originHeight=664&originWidth=836&originalType=binary&ratio=1&rotation=0&showTitle=false&size=98440&status=done&style=none&taskId=u5f4edc1d-b0dc-4b34-824c-a830a054087&title=&width=473)

Bean

- Bean的生命周期说一下？
- Spring的Bean有没有多线程问题？
- Bean处理器、Bean工厂、context
- Bean的作用区域？（Application、session、request、singleton、prototype）
- 为什么同一层有的加`@Bean`有的不加
- Spring中的Bean都是单例模式吗？service、controller、pojo、mapper类是Bean吗？
- prototype 的Bean是什么意思

[Spring事务](https://tobebetterjavaer.com/sidebar/sanfene/spring.html#%E4%BA%8B%E5%8A%A1)（这个似乎一般不会问）

- Spring 事务的本质其实就是数据库对事务的支持，没有数据库的事务支持，Spring 是无法提供事务功能的。Spring 只提供统一事务管理接口，具体实现都是由各数据库自己实现，数据库事务的提交和回滚是通过数据库自己的事务机制实现。
- Spring 事务的种类？
   - Spring 支持编程式事务管理和声明式事务管理两种方式：
- Spring 的事务隔离级别？
   - Spring 的接口 TransactionDefinition 中定义了表示隔离级别的常量，当然其实主要还是对应数据库的事务隔离级别：
- Spring 的事务传播机制？
   - Spring 事务的传播机制说的是，当多个事务同时存在的时候——一般指的是多个事务方法相互调用时，Spring 如何处理这些事务的行为。
   - 事务传播机制是使用简单的 ThreadLocal 实现的，所以，如果调用的方法是在新线程调用的，事务传播实际上是会失效的。
- 声明式事务实现原理了解吗？
   - 就是通过 AOP/动态代理。
- 声明式事务在哪些情况下会失效？
   1. @Transactional 应用在非 public 修饰的方法上
   2. @Transactional 注解属性 propagation 设置错误
   3. @Transactional 注解属性 rollbackFor 设置错误
   4. 同一个类中方法调用，导致@Transactional 失效

MVC

- Spring MVC 的核心组件？
   1. DispatcherServlet：前置控制器，是整个流程控制的**核心**，控制其他组件的执行，进行统一调度，降低组件之间的耦合性，相当于总指挥。
   2. Handler：处理器，完成具体的业务逻辑，**相当于 Servlet** 或 Action。
   3. HandlerMapping：DispatcherServlet 接收到请求之后，通过 HandlerMapping 将不同的请求映射到不同的 Handler。
   4. HandlerInterceptor：处理器拦截器，是一个接口，如果需要完成一些拦截处理，可以实现该接口。
   5. HandlerExecutionChain：处理器执行链，包括两部分内容：Handler 和 HandlerInterceptor（系统会有一个默认的 HandlerInterceptor，如果需要额外设置拦截，可以添加拦截器）。
   6. HandlerAdapter：处理器适配器，Handler 执行业务方法之前，需要进行一系列的操作，包括表单数据的验证、数据类型的转换、将表单数据封装到 JavaBean 等，这些操作都是由 HandlerApater 来完成，开发者只需将注意力集中业务逻辑的处理上，DispatcherServlet 通过 HandlerAdapter 执行不同的 Handler。
   7. ModelAndView：装载了模型数据和视图信息，作为 Handler 的处理结果，返回给 DispatcherServlet。
   8. ViewResolver：视图解析器，DispatcheServlet 通过它将逻辑视图解析为物理视图，最终将渲染结果响应给客户端。
- Spring MVC 的工作流程？

![Spring MVC的工作流程](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683699436611-3b956757-d64e-4f0d-9b7b-751df4f5af4e.png#averageHue=%23fcf8f2&clientId=u58a0abf6-6568-4&from=paste&id=uee1296aa&originHeight=589&originWidth=1100&originalType=url&ratio=1&rotation=0&showTitle=true&size=138846&status=done&style=none&taskId=u0206f386-3b33-4e72-8ac8-c62c2883152&title=Spring%20MVC%E7%9A%84%E5%B7%A5%E4%BD%9C%E6%B5%81%E7%A8%8B "Spring MVC的工作流程")

   1. 客户端向服务端发送一次请求，这个请求会先到前端控制器 DispatcherServlet(也叫中央控制器)。
   2. DispatcherServlet 接收到请求后会调用 HandlerMapping 处理器映射器。由此得知，该请求该由哪个 Controller 来处理（并未调用 Controller，只是得知）
   3. DispatcherServlet 调用 HandlerAdapter 处理器适配器，告诉处理器适配器应该要去执行哪个 Controller
   4. HandlerAdapter 处理器适配器去执行 Controller 并得到 ModelAndView(数据和视图)，并层层返回给 DispatcherServlet
   5. DispatcherServlet 将 ModelAndView 交给 ViewReslover 视图解析器解析，然后返回真正的视图。
   6. DispatcherServlet 将模型数据填充到视图中
   7. DispatcherServlet 将结果响应给客户端
   - Spring MVC 虽然整体流程复杂，但是实际开发中很简单，大部分的组件不需要开发人员创建和管理，只需要通过配置文件的方式完成配置即可，真正需要开发人员进行处理的只有 Handler（Controller） 、View 、Model。
   - 当然我们现在大部分的开发都是前后端分离，Restful 风格接口，后端只需要返回 Json 数据就行了。
- SpringMVC Restful 风格的接口的流程是什么样的呢？
   - PS:这是一道全新的八股，毕竟 ModelAndView 这种方式应该没人用了吧？现在都是前后端分离接口，八股也该更新换代了。
   - 我们都知道 Restful 接口，响应格式是 json，这就用到了一个常用注解：`@ResponseBody`
   - 加入了这个注解后，整体的流程上和使用 ModelAndView 大体上相同，但是细节上有一些不同：
      1. 客户端向服务端发送一次请求，这个请求会先到前端控制器 DispatcherServlet
      2. DispatcherServlet 接收到请求后会调用 HandlerMapping 处理器映射器。由此得知，该请求该由哪个 Controller 来处理
      3. DispatcherServlet 调用 HandlerAdapter 处理器适配器，告诉处理器适配器应该要去执行哪个 Controller
      4. Controller 被封装成了 ServletInvocableHandlerMethod，HandlerAdapter 处理器适配器去执行 invokeAndHandle 方法，完成对 Controller 的请求处理
      5. HandlerAdapter 执行完对 Controller 的请求，会调用 HandlerMethodReturnValueHandler 去处理返回值，主要的过程：
         1. 调用 RequestResponseBodyMethodProcessor，创建 ServletServerHttpResponse（Spring 对原生 ServerHttpResponse 的封装）实例
         2. 使用 HttpMessageConverter 的 write 方法，将返回值写入 ServletServerHttpResponse 的 OutputStream 输出流中【TODO：以前的JavaWeb似乎是把View写入输出流？还是这两个response不一样？】
         3. 在写入的过程中，会使用 JsonGenerator（默认使用 Jackson 框架）对返回值进行 Json 序列化
      6. 执行完请求后，返回的 ModealAndView 为 null，ServletServerHttpResponse 里也已经写入了响应，所以不用关心 View 的处理

Spring Boot

- 介绍一下 SpringBoot，有哪些优点？
   - Spring Boot 基于 Spring 开发，Spirng Boot 本身并不提供 Spring 框架的核心特性以及扩展功能，只是用于快速、敏捷地开发新一代基于 Spring 框架的应用程序。它并不是用来替代 Spring 的解决方案，而是和 Spring 框架紧密结合用于提升 Spring 开发者体验的工具。
   - Spring Boot 以约定大于配置核心思想开展工作，相比 Spring 具有如下优势：
      1. Spring Boot 可以快速创建独立的 Spring 应用程序。
      2. Spring Boot 内嵌了如 Tomcat，Jetty 和 Undertow 这样的容器，也就是说可以直接跑起来，用不着再做部署工作了。
      3. Spring Boot 无需再像 Spring 一样使用一堆繁琐的 xml 文件配置。
      4. Spring Boot 可以自动配置(核心)Spring。SpringBoot 将原有的 XML 配置改为 Java 配置，将 bean 注入改为使用注解注入的方式(@Autowire)，并将多个 xml、properties 配置浓缩在一个 appliaction.yml 配置文件中。
      5. Spring Boot 提供了一些现有的功能，如量度工具，表单数据验证以及一些外部配置这样的一些第三方功能。
      6. Spring Boot 可以快速整合常用依赖（开发库，例如 spring-webmvc、jackson-json、validation-api 和 tomcat 等），提供的 POM 可以简化 Maven 的配置。当我们引入核心依赖时，SpringBoot 会自引入其他依赖。【TODO：那个pom.xml不就是maven吗？】
- SpringBoot 自动配置原理了解吗？【TODO】
   - SpringBoot 开启自动配置的注解是`@EnableAutoConfiguration` ，启动类上的注解`@SpringBootApplication`是一个复合注解，包含了@EnableAutoConfiguration：

![SpringBoot自动配置原理](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683699965688-86e6828d-c023-4ac6-bc2a-835dbf0e8a04.png#averageHue=%23f3f2e3&clientId=u58a0abf6-6568-4&from=paste&id=u5703d1e5&originHeight=395&originWidth=1175&originalType=url&ratio=1&rotation=0&showTitle=true&size=74717&status=done&style=none&taskId=u4bbd4e87-69a9-4477-a400-af9a9170ab1&title=SpringBoot%E8%87%AA%E5%8A%A8%E9%85%8D%E7%BD%AE%E5%8E%9F%E7%90%86 "SpringBoot自动配置原理")

   - `EnableAutoConfiguration` 只是一个简单的注解，自动装配核心功能的实现实际是通过 `AutoConfigurationImportSelector`类
   - `AutoConfigurationImportSelector`实现了`ImportSelector`接口，这个接口的作用就是收集需要导入的配置类，配合`@Import()`就可以将相应的类导入到 Spring 容器中
   - 获取注入类的方法是 selectImports()，它实际调用的是`getAutoConfigurationEntry`，这个方法是获取自动装配类的关键，主要流程可以分为这么几步：
      1. 获取注解的属性，用于后面的排除
      2. **获取所有需要自动装配的配置类的路径**：这一步是最关键的，从 META-INF/spring.factories 获取自动配置类的路径
      3. 去掉重复的配置类和需要排除的重复类，把需要自动加载的配置类的路径存储起来
- 如何自定义一个 SpringBoot Srarter?
   - 知道了自动配置原理，创建一个自定义 SpringBoot Starter 也很简单。
   1. 创建一个项目，命名为 demo-spring-boot-starter，引入 SpringBoot 相关依赖
   2. 编写配置文件。这里定义了属性配置的前缀
   3. 自动装配。创建自动配置类 HelloPropertiesConfigure
   4. 配置自动类。在`/resources/META-INF/spring.factories`文件中添加自动配置类路径
   5. 测试
      - 创建一个工程，引入自定义 starter 依赖
      - 在配置文件里添加配置
      - 测试类
- Springboot 启动原理？
   - SpringApplication 这个类主要做了以下四件事情：
      1. 推断应用的类型是普通的项目还是 Web 项目
      2. 查找并加载所有可用初始化器 ， 设置到 initializers 属性中
      3. 找出所有的应用程序监听器，设置到 listeners 属性中
      4. 推断并设置 main 方法的定义类，找到运行的主类

![SpringBoot 启动大致流程-图片来源网络](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683701516357-b9264920-764b-4e59-a040-72c1096828ef.png#averageHue=%23f7f6f5&clientId=u58a0abf6-6568-4&from=paste&id=u173d718c&originHeight=1096&originWidth=1080&originalType=url&ratio=1&rotation=0&showTitle=true&size=274602&status=done&style=none&taskId=uf013b361-5a43-453d-a298-5a8bf06ebd0&title=SpringBoot%20%E5%90%AF%E5%8A%A8%E5%A4%A7%E8%87%B4%E6%B5%81%E7%A8%8B-%E5%9B%BE%E7%89%87%E6%9D%A5%E6%BA%90%E7%BD%91%E7%BB%9C "SpringBoot 启动大致流程-图片来源网络")
Spring Cloud

- 对 SpringCloud 了解多少？
   - SpringCloud 是 Spring 官方推出的微服务治理框架。
   - 什么是微服务？
      1. 2014 年 Martin Fowler 提出的一种新的架构形式。微服务架构是一种架构模式，提倡将单一应用程序划分成一组小的服务，服务之间相互协调，互相配合，为用户提供最终价值。每个服务运行在其独立的进程中，服务与服务之间采用轻量级的通信机制(如 HTTP 或 Dubbo)互相协作，每个服务都围绕着具体的业务进行构建，并且能够被独立的部署到生产环境中，另外，应尽量避免统一的，集中式的服务管理机制，对具体的一个服务而言，应根据业务上下文，选择合适的语言、工具(如 Maven)对其进行构建。
      2. 微服务化的核心就是将传统的一站式应用，根据业务拆分成一个一个的服务，彻底地去耦合，每一个微服务提供单个业务功能的服务，一个服务做一件事情，从技术角度看就是一种小而独立的处理过程，类似进程的概念，能够自行单独启动或销毁，拥有自己独立的数据库。
   - 微服务架构主要要解决哪些问题？
      1. 服务很多，客户端怎么访问，如何提供对外网关?
      2. 这么多服务，服务之间如何通信? HTTP 还是 RPC?
      3. 这么多服务，如何治理? 服务的注册和发现。
      4. 服务挂了怎么办？熔断机制。
   - 有哪些主流微服务框架？
      1. Spring Cloud Netflix
      2. Spring Cloud Alibaba
      3. SpringBoot + Dubbo + ZooKeeper
   - SpringCloud 有哪些核心组件？

![image.png](https://cdn.nlark.com/yuque/0/2023/png/12911703/1683702514180-0e9ea121-1210-45ab-a9b9-8121ad1944dd.png#averageHue=%23fcfaf9&clientId=u58a0abf6-6568-4&from=paste&id=u79455b0b&originHeight=3368&originWidth=2776&originalType=url&ratio=1&rotation=0&showTitle=false&size=997580&status=done&style=none&taskId=uc6098dae-8eac-4438-99f9-ec3cc928141&title=)

其它


MyBatis

- Mybatis中"#"和"$"的区别（传参、拼接sql等）
- Mybatis新建一个查询要做哪些事
- Mybatix有缓存吗？
- Mybatis如何将mapper.xml中的sql和Mapper接口的方法对应
- 为什么数据库表前面要加"t_"前缀：[《数据库表名前缀》](https://juejin.cn/s/%E6%95%B0%E6%8D%AE%E5%BA%93%E8%A1%A8%E5%90%8D%E5%89%8D%E7%BC%80)——使用表名前缀可以使不同类型的表更容易区分，也可以避免表名之间的冲突。

分布式

- 分布式事务一定不能使用`@Transactional`注解吗？
- 设计一个分布式锁，要注意哪些方面。怎么用redis实现

微服务

- 简单介绍一下nacos

认证授权

- 说下Jwt和token（jwt是用来生成token，可以解析出
- Jwt生成过程？
- Jwt组成部分，数据在哪一部分

RabbitMq：

- RabbitMq如何避免消息丢失（三个阶段）
- 如何保证消息的顺序性
- 如何保证消息不被重复消费

Kafka：

- Kafka如何保证顺序（Producer指定Partition，或者一个Partition）
- Kafka是pull还是push，pull有什么问题？如何解决？（pull可能造成服务端压力过大，有很多空轮询，但有个参数可以设置，类似长轮询。还有轮询时间太大的话，延迟会比较高。push可能无法知道下游的消费情况，造成过载，但是比较实时）
### 微观
> 参考：尚硅谷-SSM整合.pdf、


注解：

- SpringBoot主类注解
- SpringBoot核心配置文件
- `@Controller`、`@Service`、`@Repository`这三个注解只是在@Component注解 的基础上起了三个新的名字。  
- `@Autowired`：（Spring）在成员变量上直接标记@Autowired注解即可完成自动装配，不需要提供setXxx()方法。可以标记在构造器和set方法上  
   -  @Autowired工作流程 ： 首先根据所需要的组件类型到IOC容器中查找 
      - 能够找到唯一的bean：直接执行装配 
      - 如果完全找不到匹配这个类型的bean：装配失败 
      - 和所需类型匹配的bean不止一个
         - 没有@Qualifier注解：根据@Autowired标记位置成员变量的变量名作为bean的id进行 匹配 
            - 能够找到：执行装配 
            - 找不到：装配失败 
         - 使用@Qualifier注解：根据@Qualifier注解中指定的名称作为bean的id进行匹配 
            - 能够找到：执行装配
            - 找不到：装配失败  
   - @Autowired中有属性required，默认值为true，因此在自动装配无法找到相应的bean时，会装 配失败 。可以将属性required的值设置为true，则表示能装就装，装不上就不装，此时自动装配的属性为 默认值。
- `@Transactional`：（Jdbc相关）。 通过注解@Transactional所标识的方法或标识的类中所有的方法，都会被事务管理器管理事务。 因为service层表示业务逻辑层，一个方法表示一个完成的功能，因此处理事务一般在service层处理。事务属性：只读、超时、回顾策略、事务隔离级别、事务传播行为。
- `@RequestMapping`： （SpringMVC）@RequestMapping注解的作用就是将请求和处理请求的控制器方法关联 起来，建立映射关系。SpringMVC 接收到指定的请求，就会来找到在映射关系中对应的控制器方法来处理这个请求。
   - 属性：
      - value；
      - method（通过请求的请求方式（get或post）匹配请求映射。 对于处理指定请求方式的控制器方法。 SpringMVC中提供了派生注解如 `@GetMapping`等）；请求地址不满足method属性，报错405。
      - params（可以通过四种表达式设置请求参数 和请求映射的匹配关系）。请求地址不满足params属性，报错400。
      - headers（通过请求的请求头信息匹配请求映射）请求不满足headers属性，报错404。
   - 支持ant风格的路径
   - 支持路径中的占位符。
- Spring常用注解：
   - Web:
      - @Controller：组合注解（组合了@Component 注解），应用在 MVC 层（控制层）。
      - @RestController：该注解为一个组合注解，相当于@Controller 和@ResponseBody 的组合，注解在类上，意味着，该 Controller 的所有方法都默认加上了@ResponseBody。
      - @RequestMapping：用于映射 Web 请求，包括访问路径和参数。如果是 Restful 风格接口，还可以根据请求类型使用不同的注解：
         - @GetMapping
         - @PostMapping
         - @PutMapping
         - @DeleteMapping
      - @ResponseBody：支持将返回值放在 response 内，而不是一个页面，通常用户返回 json 数据。
      - @RequestBody：允许 request 的参数在 request 体中，而不是在直接连接在地址后面。
      - @PathVariable：用于接收路径参数，比如 @RequestMapping(“/hello/{name}”)申明的路径，将注解放在参数中前，即可获取该值，通常作为 Restful 的接口实现方法。
      - @RestController：该注解为一个组合注解，相当于@Controller 和@ResponseBody 的组合，注解在类上，意味着，该 Controller 的所有方法都默认加上了@ResponseBody。
   - 容器:
      - @Component：表示一个带注释的类是一个“组件”，成为 Spring 管理的 Bean。当使用基于注解的配置和类路径扫描时，这些类被视为自动检测的候选对象。同时@Component 还是一个元注解。
      - @Service：组合注解（组合了@Component 注解），应用在 service 层（业务逻辑层）。
      - @Repository：组合注解（组合了@Component 注解），应用在 dao 层（数据访问层）。
      - @Autowired：Spring 提供的工具（由 Spring 的依赖注入工具（BeanPostProcessor、BeanFactoryPostProcessor）自动注入）。
      - @Qualifier：该注解通常跟 @Autowired 一起使用，当想对注入的过程做更多的控制，@Qualifier 可帮助配置，比如两个以上相同类型的 Bean 时 Spring 无法抉择，用到此注解
      - @Configuration：声明当前类是一个配置类（相当于一个 Spring 配置的 xml 文件）
      - @Value：可用在字段，构造器参数跟方法参数，指定一个默认值，支持 #{} 跟 \${} 两个方式。一般将 SpringbBoot 中的 application.properties 配置的属性值赋值给变量。
      - @Bean：注解在方法上，声明当前方法的返回值为一个 Bean。返回的 Bean 对应的类中可以定义 init()方法和 destroy()方法，然后在@Bean(initMethod=”init”,destroyMethod=”destroy”)定义，在构造之后执行 init，在销毁之前执行 destroy。
      - @Scope:定义我们采用什么模式去创建 Bean（方法上，得有@Bean） 其设置类型包括：Singleton 、Prototype、Request 、 Session、GlobalSession。
   - AOP:
      - @Aspect:声明一个切面（类上） 使用@After、@Before、@Around 定义建言（advice），可直接将拦截规则（切点）作为参数。
         - @After ：在方法执行之后执行（方法上）。
         - @Before： 在方法执行之前执行（方法上）。
         - @Around： 在方法执行之前与之后执行（方法上）。
         - @PointCut： 声明切点 在 java 配置类中使用@EnableAspectJAutoProxy 注解开启 Spring 对 AspectJ 代理的支持（类上）。
   - 事务：
      - @Transactional：在要开启事务的方法上使用@Transactional 注解，即可声明式开启事务。


- boot的start
- SpringMVC中的视图View就是网页吗？
- 为什么controller层的参数可以有Model，HttpServletRequest，HttpServletResponse，这个Model和MVC里的model是一个吗？
- 那么秒杀系统中后来改为所谓静态资源有什么区别，只是说不需要每次http请求都要发送html吗？如果是的话，那这种还是MVC吗？





