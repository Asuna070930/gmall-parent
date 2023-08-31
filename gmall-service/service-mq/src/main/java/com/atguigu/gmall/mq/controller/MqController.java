package com.atguigu.gmall.mq.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mq")
public class MqController {

    @Resource
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/sendDelay")
    public Result sendDelay() {

        //时间格式化对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        rabbitService.sendDelayMessage(DelayedMqConfig.exchange_delay,
                DelayedMqConfig.routing_delay,
                "我是延迟插件消息", 10);
//        rabbitTemplate.convertAndSend(
//                DelayedMqConfig.exchange_delay,
//                DelayedMqConfig.routing_delay,
//                "我是延迟插件发送的消息",message->{
//                    //设置延迟时间
//                    message.getMessageProperties().setDelay(10*1000);
//
//                    return message;
//                });
        System.out.println("消息发送时间：\t" + simpleDateFormat.format(new Date()));

        return Result.ok();
    }


    @GetMapping("/sendDeadLetter")
    public Result sendDeadLetter() {

        //时间格式化对象
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //发送消息
        rabbitService.sendMessage(
                DeadLetterMqConfig.exchange_dead,
                DeadLetterMqConfig.routing_dead_1,
                "我是延迟消息死信队列实现的。。。。");

        //发送的时间
        System.out.println("消息发送成功时间是：\t" + simpleDateFormat.format(new Date()));

        return Result.ok();
    }

    @GetMapping("/sendConfirm")
    public Result sendConfirm() {


        rabbitService.sendMessage("exchange.confirm", "routingkey.confirm", "我是确认机制的消息");
        return Result.ok();
    }
}
