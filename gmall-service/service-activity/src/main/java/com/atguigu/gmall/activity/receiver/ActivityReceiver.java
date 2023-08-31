package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.cache.CacheHelper;
import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import net.bytebuddy.asm.Advice;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.unit.DataUnit;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ActivityReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RedisTemplate redisTemplate;



    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_TASK_18,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    @SneakyThrows
    public void clearRedisData(Message message, Channel channel){

        try {
            //构建条件对象
            QueryWrapper<SeckillGoods> queryWrapper=new QueryWrapper<>();
            queryWrapper.eq("status","1");
            queryWrapper.le("end_time",new Date());
            //查询秒杀时间结束的商品
            List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectList(queryWrapper);
            //判断
            if(!CollectionUtils.isEmpty(seckillGoods)){

                for (SeckillGoods seckillGood : seckillGoods) {
                    //清除商品列表数据
                    redisTemplate.opsForHash().delete(RedisConst.SECKILL_GOODS,seckillGood.getSkuId().toString());
                    //清除临时订单数据
                    redisTemplate.delete(RedisConst.SECKILL_ORDERS);
                    //清除秒杀订单数据
                    redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
                    //清除库存
                    redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGood.getSkuId());

                }

            }
        } catch (Exception e) {
            //收集问题，通知处理人
          e.printStackTrace();
        }


        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 秒杀下单处理
     * @param userRecode
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true",autoDelete = "false") ,
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER) ,
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    @SneakyThrows
    public void seckillUser(UserRecode userRecode,Message message,Channel channel){

        try {
            //判断
            if(userRecode!=null){

                System.out.println(userRecode);

                seckillGoodsService.seckillUser(userRecode.getSkuId(),userRecode.getUserId());
            }
        } catch (Exception e) {
            //通知管理人员
           e.printStackTrace();
        }


        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }





    /**
     * 秒杀商品缓存预热
     * @param message
     * @param channel
     *
     * 缓存预热实现：
     *    目的：将mysql中 的数据导入到redis
     *
     *    1.商品的列表数据和详情数据
     *    条件：
     *          1.时间 今天
     *          2.状态  1
     *              0未开始  1 可秒杀  2.秒杀结束
     *          3.库存必须大于0的
     *    存储--redis
     *    hash
     *    key :seckill_goods
     *    field: skuId
     *    value: 秒杀商品对象
     *
     *    表：seckill_goods
     *    实体类：SeckillGoods
     *
     *
     *    2.控制库存的实现
     *
     *
     *
     *    3.状态位
     *
     *
     * 秒杀中存储到redis的key说明：
     *       存储商品列表的key-hash
     *      public static final String SECKILL_GOODS = "seckill:goods";
     *      秒杀的临时订单存储
     *     public static final String SECKILL_ORDERS = "seckill:orders";
     *      秒杀订单存储
     *     public static final String SECKILL_ORDERS_USERS = "seckill:orders:users";
     *     秒杀库存数量
     *     public static final String SECKILL_STOCK_PREFIX = "seckill:stock:";
     *     控制用户抢购量
     *     public static final String SECKILL_USER = "seckill:user
     *
     *
     *
     *
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_TASK_1,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    @SneakyThrows
    public void importToRedis(Message message, Channel channel){

        System.out.println("我收到了缓存的消息");

        //1.查询符合条件秒杀商品
        QueryWrapper<SeckillGoods> queryWrapper=new QueryWrapper<>();
        //状态为1
        queryWrapper.eq("status","1");
        //库存
        queryWrapper.gt("stock_count",0);
        //时间 2023-08-19
        queryWrapper.eq("Date_format(start_time,'%Y-%m-%d')",DateUtil.formatDate(new Date()));


        List<SeckillGoods> seckillGoods = seckillGoodsMapper.selectList(queryWrapper);

        //2.将商品存储到redis
        if(!CollectionUtils.isEmpty(seckillGoods)){
            //遍历
            for (SeckillGoods seckillGood : seckillGoods) {
                //查询是否存在该商品
                BoundHashOperations<String,String ,SeckillGoods> boundHashOperations = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS);
                if(boundHashOperations.hasKey(seckillGood.getSkuId().toString())){
                   //如果存在跳出本次循环
                    continue;
                }
                 //计算商品秒杀时间的差值
                Long timeSubtract = DateUtil.getTimeSubtract(seckillGood.getEndTime(), new Date());
                //存储到redis
                boundHashOperations.put(seckillGood.getSkuId().toString(),seckillGood);
                //设置数据的超时时间
                boundHashOperations.expire(timeSubtract, TimeUnit.SECONDS);
                //遍历商品的库存数量
                for (Integer i = 0; i < seckillGood.getStockCount(); i++) {
                    //控制库存超卖
                    redisTemplate.opsForList().leftPush(RedisConst.SECKILL_STOCK_PREFIX+seckillGood.getSkuId(),seckillGood.getSkuId().toString());


                }

                //状态位 ---作用快速的响应商品是否还有库存 key:23  value :1 有库存  0 已售罄
                //需要进行状态位同步
                //redis--发布与订阅
//                CacheHelper.put(seckillGood.getSkuId().toString(),"1");

                redisTemplate.convertAndSend("seckillPush",seckillGood.getSkuId()+":1");





            }



        }







        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }


}
