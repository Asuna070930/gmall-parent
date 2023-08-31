package com.atguigu.gmall.order.config;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;


@Configuration
public class OrderCancelMqConfig {


    @Bean
    public Queue cancelQueue(){

        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }

    @Bean
    public CustomExchange cancelExchange(){
        Map<String, Object> arguments =new HashMap<>();
        arguments.put("x-delayed-type","direct");

        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,arguments);
    }

    @Bean
    public Binding cancelBinding(){

        return BindingBuilder.bind(cancelQueue()).to(cancelExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }

}
