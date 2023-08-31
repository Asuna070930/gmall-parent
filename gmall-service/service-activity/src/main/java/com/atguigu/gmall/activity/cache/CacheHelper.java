package com.atguigu.gmall.activity.cache;

import java.util.concurrent.ConcurrentHashMap;

public class CacheHelper {


    /**
     * 定义map集合存储商品的状态位
     */
    private final  static ConcurrentHashMap<String,String> cacheMap=new ConcurrentHashMap<>();


    /**
     * 商品状态位加入本地缓存
     * @param key
     * @param value
     */
    public static  void put(String key,String value){

        cacheMap.put(key,value);
    }

    /**
     * 获取指定商品的状态位
     * @param key
     * @return
     */
    public static String get(String key){
        return cacheMap.get(key);
    }

    /**
     * 移除指定商品状态位
     * @param key
     */
    public static void remover(String key){

        cacheMap.remove(key);

    }

    /**
     * 清空缓存
     */
    public static  void clear(){

        cacheMap.clear();

    }

}
