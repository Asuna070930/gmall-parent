package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;

    /**
     * 商品下架监听
     * @param skuId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    @SneakyThrows
    public void lowerGoods(Long skuId, Message message, Channel channel){

        try {
            //判断
            if(skuId!=null){
                searchService.lowerGoods(skuId);

            }
        } catch (Exception e) {
            e.printStackTrace();
            //发送短信给程序员，或者运维，写入日志，发送邮件
        }

        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);




    }
    /**
     * 商品上架监听
     * @param skuId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    @SneakyThrows
    public void upperGoods(Long skuId, Message message, Channel channel){

        try {
            //判断
            if(skuId!=null){
                searchService.upperGoods(skuId);

            }
        } catch (Exception e) {
            e.printStackTrace();
            //发送短信给程序员，或者运维，写入日志，发送邮件
        }

        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);




    }


}
