package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DeadLetterReceiver {

    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    @SneakyThrows
    public void processDeadMessage(String msg, Message message , Channel channel){

        //时间格式化对象
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("接收到的消息是:"+msg);

        System.out.println("接收到消息的时间是:\t"+simpleDateFormat.format(new Date()));

        //手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
