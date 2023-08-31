package com.atguigu.gmall.common.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.pojo.GmallCorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * 延迟消息发送
     * @param exchange
     * @param routingKey
     * @param message
     * @param delayTime
     * @return
     */
    public boolean sendDelayMessage(String exchange,String routingKey,Object message,int delayTime){

        //创建消息数据对象
        GmallCorrelationData correlationData=new GmallCorrelationData();
        //生成id
        String correlationId = UUID.randomUUID().toString().replaceAll("-", "");
        //设置id
        correlationData.setId(correlationId);
        //设置交换机
        correlationData.setExchange(exchange);
        //设置路由
        correlationData.setRoutingKey(routingKey);
        //设置消息
        correlationData.setMessage(message);
        //设置延迟类型
        correlationData.setDelay(true);
        //设置延迟时间
        correlationData.setDelayTime(delayTime);

        //存储redis
        redisTemplate.opsForValue().set(
                MqConst.MQ_KEY_PREFIX+correlationId,
                JSONObject.toJSONString(correlationData),
                30,
                TimeUnit.MINUTES);
        //发送消息
        this.rabbitTemplate.convertAndSend(exchange,routingKey,message,message1 -> {

            message1.getMessageProperties().setDelay(delayTime*1000);
            return message1;
        },correlationData);





        return true;
    }

    /**
     * 发送 消息封装
     * @param exchange
     * @param routingKey
     * @param message
     * @return
     */
    public boolean sendMessage(String exchange,String routingKey,Object message){

        //创建消息数据对象
        GmallCorrelationData correlationData=new GmallCorrelationData();
        //生成id
        String correlationId = UUID.randomUUID().toString().replaceAll("-", "");
        //设置id
        correlationData.setId(correlationId);
        //设置交换机
        correlationData.setExchange(exchange);
        //设置路由
        correlationData.setRoutingKey(routingKey);
        //设置消息
        correlationData.setMessage(message);
        //存储到redis
        redisTemplate.opsForValue().set(
                MqConst.MQ_KEY_PREFIX+correlationId,
                JSONObject.toJSONString(correlationData),
                30,
                TimeUnit.MINUTES);

        //发送消息
        rabbitTemplate.convertAndSend(exchange,routingKey,message,correlationData);

        return true;
    }

//    /**
//     * 发送 消息封装
//     * @param exchange
//     * @param routingKey
//     * @param message
//     * @return
//     */
//    public boolean sendMessage(String exchange,String routingKey,Object message){
//
//      //发送消息
//        rabbitTemplate.convertAndSend(exchange,routingKey,message);
//
//        return true;
//    }
}
