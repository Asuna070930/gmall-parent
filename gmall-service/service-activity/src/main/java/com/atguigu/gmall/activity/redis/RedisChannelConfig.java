package com.atguigu.gmall.activity.redis;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisChannelConfig {


    /**
     * 订阅主题
     * @param connectionFactory 链接工厂
     * @param listenerAdapter 监听器适配器
     * @return
     */
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        //设置链接工厂
        container.setConnectionFactory(connectionFactory);
        //设置监听器适配器
        container.addMessageListener(listenerAdapter, new PatternTopic("seckillPush"));
        return container;

    }


    /**
     * 监听器适配器
     * @return
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(ReceiverMessage receiverMessage){


        //反射---CLASS对象---method --方法执行
        return new MessageListenerAdapter(receiverMessage,"messageReceive");
    }


    @Bean
    public StringRedisTemplate template(RedisConnectionFactory factory){

        return new StringRedisTemplate(factory);
    }


}
