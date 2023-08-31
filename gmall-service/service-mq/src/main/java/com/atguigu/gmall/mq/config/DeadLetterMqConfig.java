package com.atguigu.gmall.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeadLetterMqConfig {
    // 声明一些变量
    public static final String exchange_dead = "exchange.dead";
    public static final String routing_dead_1 = "routing.dead.1";
    public static final String routing_dead_2 = "routing.dead.2";
    public static final String queue_dead_1 = "queue.dead.1";
    public static final String queue_dead_2 = "queue.dead.2";

    /**
     * 初始化交换机
     * @return
     */
    @Bean
    public DirectExchange deadExchange(){

        return new DirectExchange(exchange_dead);
    }

    /**
     * 初始化队列一
     * @return
     */
    @Bean
    public Queue queueDead1(){

        //创建参数封装对象
        Map<String, Object> arguments =new HashMap<>();
        //设置死信交换机
        arguments.put("x-dead-letter-exchange",exchange_dead);
        //设置死信路由
        arguments.put("x-dead-letter-routing-key",routing_dead_2);
        //设置消息ttl
        arguments.put("x-message-ttl",10*1000);


        return new Queue(queue_dead_1,true,false,false,arguments);
    }


    /**
     * 初始化队列二
     * @return
     */
    @Bean
    public Queue queueDead2(){



        return new Queue(queue_dead_2,true);
    }


    @Bean
    public Binding binding1(){

        return BindingBuilder.bind(queueDead1()).to(deadExchange()).with(routing_dead_1);
    }


    @Bean
    public Binding binding2(){

        return BindingBuilder.bind(queueDead2()).to(deadExchange()).with(routing_dead_2);
    }

}
