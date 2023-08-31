package com.atguigu.gmall.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {


    /**
     * 创建线程池
     * @return
     */
    @Bean
    public ThreadPoolExecutor executor(){


        return new ThreadPoolExecutor(
                50,
                500,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(10000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()

        );
    }
}
