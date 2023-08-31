package com.atguigu.gmall.mq.receiver;


import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class ConfirmReceiver {


    /**
     * 消费者监听
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = "confirm.queue",durable = "true",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm"),
            key = {"routingkey.confirm"}
    ))
    public void process(String msg, Message message, Channel channel) throws IOException {


        System.out.println("我是消费者，收到的消息是："+msg);

        //手动确认消费  参数一：消息的标识  参数二：是否批量确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        /**
         * 参数一：消息的标识
         * 参数二：是否批量确认
         * 参数三：是否放回队列
         */
//        channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);


    }


}
