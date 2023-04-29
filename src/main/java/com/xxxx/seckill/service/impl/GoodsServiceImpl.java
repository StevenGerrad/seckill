package com.xxxx.seckill.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xxxx.seckill.mapper.GoodsMapper;
import com.xxxx.seckill.pojo.Goods;
import com.xxxx.seckill.service.IGoodsService;
import com.xxxx.seckill.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zhoubin
 * @since 2023-03-06
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    /**
     * @description 返回当前的商品列表
     * @return List<GoodsVo>
     * @author wangjunyou
     * @date 2023/4/24 10:29
     */
    @Override
    public List<GoodsVo> findGoodsVo(){
        //return goodsMapper.findGoodsVo();
        List<GoodsVo> goods = goodsMapper.findGoodsVo();
        return goods;
    }

    /**
     * @description 按商品id查询商品信息
     * @param goodsId
     * defaultParamDescription
     * @return GoodsVo
     * @author wangjunyou
     * @date 2023/4/24 10:30
     */
    @Override
    public GoodsVo findGoodsVoByGoodsId(Long goodsId){
        return goodsMapper.findGoodsVoByGoodsId(goodsId);
    }
}
