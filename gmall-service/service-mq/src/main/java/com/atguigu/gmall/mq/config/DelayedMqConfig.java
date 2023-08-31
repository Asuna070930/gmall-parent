package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DelayedMqConfig {


    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    @Bean
    public Queue delayQueue(){

       return new Queue(queue_delay_1,true);
    }

    @Bean
    public CustomExchange delayExchange(){
        Map<String, Object> arguments =new HashMap<>();
        //设置使用的模式
        arguments.put("x-delayed-type","direct");

        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,arguments);
    }

    @Bean
    public Binding delayBinding(){

        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }
}
