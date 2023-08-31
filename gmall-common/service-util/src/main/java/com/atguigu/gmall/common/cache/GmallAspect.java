package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;


    /**
     * 切面编写;
     * @Around("com.xyz.myapp.CommonPointcuts.businessService()")
     *@Around:通知 环绕通知
     * 切点表达式：
     *com.xyz.myapp.CommonPointcuts.businessService()
     *
     * 注解切点表达式的写法：
     * @annotation(com.atguigu.gmall.common.cache.GmallCache)
     *
     * @param joinPoint
     * @return
     * @throws Throwable
     *
     * 分布式锁+缓存
     *  1.定义数据key,尝试从redis中获取数据
     *   有，返回
     *   2.没有，定义锁，获取锁
     *      3.没有，自旋
     *      4.有，查询数据库
     *         5.有存储到redis 返回
     *         6.没有，存储空值，返回
     *     7.释放锁
     *
     *  兜底防范
     *  查询mysql
     *
     *
     *
     */
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheGmallAspect(ProceedingJoinPoint joinPoint) throws Throwable {
        Object object= null;
        Object[] args =null;
        try {
            object = new Object();
            //定义数据key 例如：sku:21:info
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            //获取注解对象
            GmallCache annotation = signature.getMethod().getAnnotation(GmallCache.class);
            //获取前缀属性
            String prefix = annotation.prefix();
            //获取后缀属性
            String suffix = annotation.suffix();
            //获取参数
             args = joinPoint.getArgs();
            //数据key sku:[21]:info
            String dataKey=prefix+ Arrays.toString(args) +suffix;
            //获取当前方法的返回值类型
            Class returnType = signature.getReturnType();
            //查询redis
            object=cacheHit(dataKey,returnType);
            //判断
            if(object==null){ //此时缓存没有数据，查询数据库
                //定义lockKey sku:[21]:lock
                String lockKey=prefix+ Arrays.toString(args) + RedisConst.SKULOCK_SUFFIX;
                //获取锁
                RLock lock = redissonClient.getLock(lockKey);
                //加锁
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                //判断
                if(flag){//获取到了锁
                    try {
                        //可以查询数据库，实际就是执行被增强方法的方法体
                        object = joinPoint.proceed(args);
                        //判断
                        if(object==null){
                            //存储空值
                            object = returnType.newInstance();
                            //存储redis
                            redisTemplate.opsForValue().set(dataKey,JSON.toJSONString(object),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);

                        }else{

                            redisTemplate.opsForValue().set(dataKey,JSON.toJSONString(object),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);

                        }

                        return object;
                    } finally {
                        //释放锁
                        lock.unlock();
                    }


                }else{
                    //没有获取到锁，睡眠和自旋
                    Thread.sleep(50);
                    return cacheGmallAspect(joinPoint);
                }


            }else{
                //缓存中有数据
                return object;
            }
        } catch (Throwable e) {
           e.printStackTrace();
        }


        //兜底方法
        return joinPoint.proceed(args);
    }

    /**
     * 从redis中获取数据
     * @param dataKey
     * @return
     */
    private Object cacheHit(String dataKey,Class returnType) {
        //获取数据
        String strJson= (String) redisTemplate.opsForValue().get(dataKey);
        //判断
        if(!StringUtils.isEmpty(strJson)){

            //转换成实际的对象--目的向上转型
            Object object = JSON.parseObject(strJson, returnType);

            return object;

        }

        return null;
    }


}
