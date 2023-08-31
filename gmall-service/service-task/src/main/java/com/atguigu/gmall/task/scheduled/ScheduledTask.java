package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
@EnableScheduling
public class ScheduledTask {


    @Resource
    private RabbitService rabbitService;

    /**
     * 秒 分 时  日 月 星期 年（可省略）
     *
     * 特殊符号：
     *       1.* 每个时间单位都执行
     *       2. , 或者
     *       3. - 区间
     *       4. 日期 和星期的问题
     *       每天凌晨
     * cron="0 0 1 * * ?"
     */
    @Scheduled(cron ="0 0 1 * * ?")
    public void task_1(){
        System.out.println("定时任务执行。。。。。。");
        //发送消息到活动模块进行缓存预热
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_TASK,
                MqConst.ROUTING_TASK_1,
                ""
        );


    }

    /**
     * 缓存数据清理
     */
    @Scheduled(cron ="0/10 * * * * ?")
    public void task_18(){
        System.out.println("定时任务执行清理秒杀结束数据。。。。。。");
        //发送消息到活动模块进行缓存预热
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_TASK,
                MqConst.ROUTING_TASK_18,
                ""
        );


    }





}
