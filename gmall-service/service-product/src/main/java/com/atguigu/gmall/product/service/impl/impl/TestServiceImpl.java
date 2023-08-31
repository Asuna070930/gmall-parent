package com.atguigu.gmall.product.service.impl.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Service
public class TestServiceImpl  implements TestService {
    @Autowired
    private RedissonClient redissonClient;
    // 初始化读写锁
//    private  RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
    @Autowired
    private StringRedisTemplate redisTemplate;






    @Override
    public String readLock() {
       RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.readLock(); // 获取读锁

        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁

        String msg = this.redisTemplate.opsForValue().get("msg");

        //rLock.unlock(); // 解锁
        return msg;
    }

    @Override
    public String writeLock() {
        // 初始化读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readwriteLock");
        RLock rLock = readWriteLock.writeLock(); // 获取写锁

        rLock.lock(10, TimeUnit.SECONDS); // 加10s锁

        this.redisTemplate.opsForValue().set("msg", UUID.randomUUID().toString());

        //rLock.unlock(); // 解锁
        return "成功写入了内容。。。。。。";
    }
    /**
     * 分布式锁值reddisson实现
     *
     * 前提：初始化操作对象
     *
     * 1.加锁
     *redissonClient.getLock()
     *  lock.lock();
     *  lock.lock(7, TimeUnit.SECONDS); 超时时间
     *  boolean b = lock.tryLock(10, 7, TimeUnit.SECONDS);
     *   参数一：等待获取锁的时间
     *   参数二：持有锁的时间
     *   参数三：时间单位
     *
     *
     * 2.释放锁
     *   lock.unlock();
     *
     *
     * 实现步骤：
     *
     * 1.加锁获取锁
     *      //获取锁
     *             RLock lock = redissonClient.getLock("lock");
     *             //加锁
     * //        lock.lock();
     * //        lock.lock(7, TimeUnit.SECONDS);
     *             boolean flag = lock.tryLock(10, 7, TimeUnit.SECONDS);
     *
     *  2.释放锁
     *
     *    lock.unlock();
     *
     *
     *  3.自旋
     *   Thread.sleep(100);
     *    testLock();
     */
    @Override
    public  synchronized void testLock() {

        try {
            //获取锁
            RLock lock = redissonClient.getLock("lock");
            //加锁
//        lock.lock();
//        lock.lock(7, TimeUnit.SECONDS);
            boolean flag = lock.tryLock(10, 7, TimeUnit.SECONDS);
            //判断
            if(flag){
                try {
                    //1.从redis中获取数据num
                    String num = redisTemplate.opsForValue().get("num");
                    //判断
                    if(StringUtils.isEmpty(num)){
                        return ;
                    }
                    //2.转换类型
                    int number = Integer.parseInt(num);
                    //3.自增存储
                    redisTemplate.opsForValue().set("num",String.valueOf(++number));
                } finally {
                    lock.unlock();
                }


            }else{

                Thread.sleep(100);
                testLock();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }


    /**
     * redis实现分布式锁
     *
     * 实现步骤：
     *  1.通过setnx获取锁
     *  2.判断是否获取成功
     *     3.成功直接操作num++
     *     4.失败 睡眠 自旋
     *
     *
     *
     * 分布式锁的实现总结：
     *  1.加锁
     *     //生成uuid
     *         String uuid= UUID.randomUUID().toString().replace("-","");
     *         //通过redis的setnx获取锁  set nx不存在 ex秒级存活时间
     *         Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 7, TimeUnit.SECONDS);
     *
     *  2.释放锁
     *
     *   //使用lua脚本改造
     *             String script="if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
     *                     "then\n" +
     *                     "    return redis.call(\"del\",KEYS[1])\n" +
     *                     "else\n" +
     *                     "    return 0\n" +
     *                     "end";
     *             //创建脚本对象
     *             DefaultRedisScript redisScript=new DefaultRedisScript();
     *             //设置脚本
     *             redisScript.setScriptText(script);
     *             //设置返回值类型
     *             redisScript.setResultType(Long.class);
     *             //发送脚本
     *             redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
     *  3.自旋
     *              Thread.sleep(100);
     *                 //自旋
     *                 testLock();
     *
     *
     */
//    @Override
//    public void testLock() {
//
//        //通过redis的setnx获取锁 setnx
////        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", "222");
////        //设置超时时间7
////        redisTemplate.expire("lock",7, TimeUnit.SECONDS);
//        //生成uuid
//        String uuid= UUID.randomUUID().toString().replace("-","");
//        //通过redis的setnx获取锁  set nx不存在 ex秒级存活时间
//        Boolean flag = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 7, TimeUnit.SECONDS);
//        //判断获取锁的结果
//        if(flag){
//            //1.从redis中获取数据num
//            String num = redisTemplate.opsForValue().get("num");
//            //判断
//            if(StringUtils.isEmpty(num)){
//                return ;
//            }
//            //2.转换类型
//            int number = Integer.parseInt(num);
//            //3.自增存储
//            redisTemplate.opsForValue().set("num",String.valueOf(++number));
//
//            //获取lock的锁值
//            String lock = redisTemplate.opsForValue().get("lock");
//            //使用lua脚本改造
//            String script="if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
//                    "then\n" +
//                    "    return redis.call(\"del\",KEYS[1])\n" +
//                    "else\n" +
//                    "    return 0\n" +
//                    "end";
//            //创建脚本对象
//            DefaultRedisScript redisScript=new DefaultRedisScript();
//            //设置脚本
//            redisScript.setScriptText(script);
//            //设置返回值类型
//            redisScript.setResultType(Long.class);
//            //发送脚本
//            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);
//           /* if(uuid.equals(lock)){
//                //释放锁
//                redisTemplate.delete("lock");
//            }*/
//        }else{
//            //睡眠
//            try {
//                Thread.sleep(100);
//                //自旋
//                testLock();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//
//        }
//
//
//
//
//    }


//    /**
//     * 测试锁的局限性
//     */
//    @Override
//    public  synchronized void testLock() {
//
//        //1.从redis中获取数据num
//        String num = redisTemplate.opsForValue().get("num");
//        //判断
//        if(StringUtils.isEmpty(num)){
//            return ;
//        }
//        //2.转换类型
//        int number = Integer.parseInt(num);
//        //3.自增存储
//        redisTemplate.opsForValue().set("num",String.valueOf(++number));
//
//
//    }
}
