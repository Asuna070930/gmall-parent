package com.atguigu.gmall.mq.receiver;

import com.atguigu.gmall.mq.config.DelayedMqConfig;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class DelayReceiver {

    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    /**
     * 保证消息消费幂等性处理
     * 消息消费的幂等性
     *
     * 思路：给消费添加唯一记录
     *
     * redis ---setnx
     *
     * 1.存储 setnx
     * key :msg  value:0或者1
     * 2.问题：
     *  在消息执行setnx后消费消息之前保存了，导致消息未消费，造成消息丢失，数据不一致性出现
     *  解决：value 值：0  完成消费后更新未1
     *
     *
     *
     * @param msg
     * @param message
     * @param channel
     */
    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    @SneakyThrows
    public void processDelay(String msg, Message message, Channel channel){

        //此时可以用消息数据对象id做唯一的key
//        System.out.println(message.getMessageProperties().getHeaders().get("spring_returned_message_correlation"));

        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //存储消息，保证幂等性
        Boolean result = redisTemplate.opsForValue().setIfAbsent("delay:" + msg, "0", 20, TimeUnit.SECONDS);
        //判断
        if(result){
            //第一次消费
            //时间格式化对象
            System.out.println("接收到的消息是："+msg);

            System.out.println("接收消息的时间是："+simpleDateFormat.format(new Date()));
            this.redisTemplate.opsForValue().set("delay:" + msg,"1");

        }else{
            //此时，如果result的结果未false，只能表示消息来过，不能表示消费成功
            String value = redisTemplate.opsForValue().get("delay:" + msg);
            //判断 value=0表示只是消息来过，当时未消费成功，再次消费
            if("0".equals(value)){
                //时间格式化对象

                System.out.println("接收到的消息是："+msg);
                System.out.println("接收消息的时间是："+simpleDateFormat.format(new Date()));
                this.redisTemplate.opsForValue().set("delay:" + msg,"1");

                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

            }

        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);


    }
}
