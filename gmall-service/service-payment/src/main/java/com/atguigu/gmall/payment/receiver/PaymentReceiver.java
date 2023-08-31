package com.atguigu.gmall.payment.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import com.rabbitmq.client.Channel;
import io.netty.util.internal.UnstableApi;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentReceiver {

    @Autowired
    private PaymentInfoService paymentInfoService;

    /**
     * 订单超时，关闭交易记录
     * @param orderId
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_PAYMENT_CLOSE,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE),
            key = {MqConst.ROUTING_PAYMENT_CLOSE}
    ))
    @SneakyThrows
    public void closePayment(Long orderId, Message message, Channel channel){

        try {
            //判断
            if(orderId!=null){
                //修改状态为CLOSED
                paymentInfoService.closedPayment(orderId);


            }
        } catch (Exception e) {

            //通知处理
            e.printStackTrace();

        }


        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
