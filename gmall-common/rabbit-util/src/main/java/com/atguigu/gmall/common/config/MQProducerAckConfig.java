package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.pojo.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MQProducerAckConfig implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {


    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * @PostConstruct是Java自带的注解， 在方法上加该注解会在项目启动的时候执行该方法，
     * 也可以理解为在spring容器初始化的时候执行该方法
     * 要求：
     * 修饰一个非静态的void（）方法,在服务器加载Servlet的时候运行，
     * 并且只会被服务器执行一次在构造函数之后执行，init（）方法之前执行。
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);            //指定 ConfirmCallback
        rabbitTemplate.setReturnCallback(this);             //指定 ReturnCallback
    }

    /**
     * @Description 消息发送确认
     * <p>
     *

     * <p>
     *
     */

    /**
     * ConfirmCallback  只确认消息是否正确到达 Exchange 中
     * 如果消息没有到exchange,则confirm回调,ack=false
     * 如果消息到达exchange,则confirm回调,ack=true
     *
     * @param correlationData correlation data for the callback.
     * @param ack             true for ack, false for nack
     * @param cause           An optional cause, for nack, when available, otherwise null.
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

        if (ack) {
            log.info("消息发送成功：" + JSON.toJSONString(correlationData));
        } else {
            log.info("消息发送失败：" + cause + " 数据：" + JSON.toJSONString(correlationData));
            //重试发送
            this.retrySendMsg(correlationData);
        }


    }


    /**
     * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
     * exchange到queue成功,则不回调return
     * exchange到queue失败,则回调return
     *
     * @param message    the returned message.
     * @param replyCode  the reply code.
     * @param replyText  the reply text.
     * @param exchange   the exchange.
     * @param routingKey the routing key.
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        //获取消息数据对象id
        String correlationId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");

        //从redis中获取数据对象
        String correJson = (String) redisTemplate.opsForValue().get(MqConst.MQ_KEY_PREFIX+correlationId);
        //转换类型
        if (!StringUtils.isEmpty(correJson)) {
            GmallCorrelationData correlationData = JSONObject.parseObject(correJson, GmallCorrelationData.class);
            //重试发送
            this.retrySendMsg(correlationData);

        }

    }


    /**
     * 消息重试机制
     *
     * @param correlationData
     */
    private void retrySendMsg(CorrelationData correlationData) {
        //转换类型
        GmallCorrelationData gmallCorrelationData = (GmallCorrelationData) correlationData;
        //获取重试次数 默认是0
        int retryCount = gmallCorrelationData.getRetryCount();
        //判断
        if (retryCount >= MqConst.RETRY_COUNT) {

            //  不需要重试了
            log.error("重试次数已到，发送消息失败:" + JSON.toJSONString(gmallCorrelationData));

        } else {
            //更新次数
            retryCount++;
            System.out.println("重试次数：\t"+retryCount);
            //设置次数
            gmallCorrelationData.setRetryCount(retryCount);

            //存储到redis
            redisTemplate.opsForValue().set(
                    MqConst.MQ_KEY_PREFIX+gmallCorrelationData.getId(),
                    JSONObject.toJSONString(gmallCorrelationData),
                    30,
                    TimeUnit.MINUTES);
            if(gmallCorrelationData.isDelay()){
                //延迟消息重试
                //发送消息
                this.rabbitTemplate.convertAndSend(
                        gmallCorrelationData.getExchange(),
                        gmallCorrelationData.getRoutingKey(),
                        gmallCorrelationData.getMessage(),message1 -> {

                    message1.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
                    return message1;
                },gmallCorrelationData);



            }else{
                //发送消息--普通消息重试
                rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange()
                        , gmallCorrelationData.getRoutingKey(),
                        gmallCorrelationData.getMessage(),
                        gmallCorrelationData);
            }


        }


    }
}
