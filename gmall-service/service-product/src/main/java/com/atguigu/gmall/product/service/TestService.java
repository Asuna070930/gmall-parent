package com.atguigu.gmall.product.service;

public interface TestService {
    /**
     * 测试锁的局限性
     */
    void testLock();


    String readLock();

    String writeLock();

}
