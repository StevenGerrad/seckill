package com.xxxx.seckill.vo;

import com.xxxx.seckill.pojo.Goods;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class GoodsVo extends Goods {
//    private BigDecimal seckillPrice;
//    private Integer stockCount;
//
//    private Date startDate;
//    private Date endDate;
//
//}

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsVo {

    private Long id;
    private String goodsName;
    private String goodsTitle;
    private String goodsImg;
    private String goodsDetail;
    private BigDecimal goodsPrice;
    private Integer goodsStock;
    private BigDecimal seckillPrice;
    private Integer stockCount;
    private Date startDate;
    private Date endDate;

}
