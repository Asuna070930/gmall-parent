package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.cache.CacheHelper;
import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    /**
     * 查询秒杀列表数据
     * @return
     */
    @Override
    public List<SeckillGoods> findAll() {
        //从redis获取数据
        List<SeckillGoods> seckillGoods = redisTemplate.opsForHash().values(RedisConst.SECKILL_GOODS);

        return seckillGoods;
    }

    /**
     * 秒杀详情页数据查询
     * @param skuId
     * @return
     */
    @Override
    public SeckillGoods getSeckillGoods(String skuId) {

        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(skuId);

        return seckillGoods;
    }

    /**
     * 秒杀抢购--加入mq
     * @param skuId
     * @param skuIdStr
     * @param userId
     * @return
     */
    @Override
    public Result seckillOrder(String skuId, String skuIdStr, String userId) {

        //校验抢购码
        String encrypt = MD5.encrypt(userId + skuId);
        //判断
        if(!encrypt.equals(skuIdStr)){
            return Result.build(null, ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //判断状态位
        String cache = CacheHelper.get(skuId);
        //判断
        if(StringUtils.isEmpty(cache)){
            return Result.build(null, ResultCodeEnum.ILLEGAL_REQUEST);
        }
        //判断值
        if("1".equals(cache)){
            //加入mq中
            UserRecode userRecode=new UserRecode();
            userRecode.setUserId(userId);
            userRecode.setSkuId(Long.parseLong(skuId));

            //发送消息
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_SECKILL_USER,
                    MqConst.ROUTING_SECKILL_USER,
                    userRecode
            );

            return Result.ok();
        }

        //已售罄
       return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
    }

    /**
     * 秒杀抢购-消费接口
     * @param skuId
     * @param userId
     */
    @Override
    public void seckillUser(Long skuId, String userId) {

        //校验状态位
        String stat = CacheHelper.get(skuId.toString());
        //判断
        if("0".equals(stat)){
            return;
        }
        //判断用户是否抢购过--setnx
        Boolean result = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId.toString(), RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        //获取商品id
        String skuIdRedis = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_USER + userId);
        if(!result&&skuId.toString().equals(skuIdRedis) ){

            return ;
        }

        //校验库存 --redis-list ,判断标准：如果list最后一个数据被取出，list会被删除
        String skuIdStock = (String) redisTemplate.opsForList().rightPop(RedisConst.SECKILL_STOCK_PREFIX + skuId);
        //判断
        if(!StringUtils.isEmpty(skuIdStock)){
            //生成临时订单，存储到redis
            OrderRecode orderRecode=new OrderRecode();
            orderRecode.setUserId(userId);
            SeckillGoods seckillGoods = getSeckillGoods(skuId.toString());
            orderRecode.setSeckillGoods(seckillGoods);
            orderRecode.setNum(1);
            orderRecode.setOrderStr(MD5.encrypt(userId+skuId));


            //存储到redis-临时
            //类型  key  value ( 1  订单  2 订单)
            redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS,userId,orderRecode);
        }else{
            //同步状态位
            redisTemplate.convertAndSend("seckillPush",skuId+":0");
            return;
        }




        //更新库存
        this.updateStockRedisAndMysql(skuId);

    }

    /**
     * 轮询查询抢购结果
     * 211----排队中
     *
     * 215----成功 未支付
     * 有标识，有临时订单
     *
     * 218---成功 支付
     *
     * 其他 ---已售罄
     *
     *
     * @param skuId
     * @return
     */
    @Override
    public Result checkOrder(String skuId,String userId) {
        //查询用户抢购的记录
        String flag = (String) redisTemplate.opsForValue().get(RedisConst.SECKILL_USER + userId);
        //判断
        if(!StringUtils.isEmpty(flag)&&flag.equals(skuId)){

            //判断一下是否有临时订单
            OrderRecode orderRecode = (OrderRecode) redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
            //判断
            if(orderRecode!=null){

                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }

            //查询是否为支付  userId---orderId
            String orderId = (String) redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS_USERS, userId);
            //判断
            if(!StringUtils.isEmpty(orderId)){
                return Result.build(orderId, ResultCodeEnum.SECKILL_ORDER_SUCCESS);

            }


        }

        //用户没有标识--还有轮到
        //判断状态位
        String stat = CacheHelper.get(skuId);
        //判断
        if("1".equals(stat)){

            return Result.build(null, ResultCodeEnum.SECKILL_RUN);
        }


        return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
    }

    /**
     * 更新库存
     * @param skuId
     *
     * 加锁？？？？
     *
     */
    private void updateStockRedisAndMysql(Long skuId) {

        //获取库存余量 list
        Long count = redisTemplate.opsForList().size(RedisConst.SECKILL_STOCK_PREFIX + skuId);
        if(count %2==0){

            //更新redis
            SeckillGoods seckillGoods = getSeckillGoods(skuId.toString());
            //更新库存
            seckillGoods.setStockCount(count.intValue());
            //更新到redis
            redisTemplate.opsForHash().put(RedisConst.SECKILL_GOODS,skuId.toString(),seckillGoods);

            //更新mysql
            seckillGoodsMapper.updateById(seckillGoods);
        }



    }
}
