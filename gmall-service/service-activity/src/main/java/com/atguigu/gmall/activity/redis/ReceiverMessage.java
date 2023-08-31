package com.atguigu.gmall.activity.redis;

import com.atguigu.gmall.activity.cache.CacheHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReceiverMessage {

    /**
     * 同步状态位消费者
     * @param message
     *
     * 接收的数据为 ““23:1””
     */
    public void messageReceive(String message){
        //判断
        if(!StringUtils.isEmpty(message)){

            //处理数据，去掉双引号
            message = message.replaceAll("\"", "");
            //切割字符串“:”
            String[] split = message.split(":");
            //判断
            if(split!=null &&split.length==2){

                CacheHelper.put(split[0],split[1]);

            }


        }



        System.out.println(message);


    }
}
