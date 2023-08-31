package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

public interface SeckillGoodsService {
    /**
     * 查询秒杀列表数据
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 秒杀详情页数据查询
     * @param skuId
     * @return
     */
    SeckillGoods getSeckillGoods(String skuId);

    /**
     * 秒杀抢购--加入mq
     * @param skuId
     * @param skuIdStr
     * @param userId
     * @return
     */
    Result seckillOrder(String skuId, String skuIdStr, String userId);

    /**
     * 秒杀抢购-消费接口
     * @param skuId
     * @param userId
     */
    void seckillUser(Long skuId, String userId);

    /**
     * 轮询查询抢购结果
     * @param skuId
     * @return
     */
    Result checkOrder(String skuId,String userId);
}
