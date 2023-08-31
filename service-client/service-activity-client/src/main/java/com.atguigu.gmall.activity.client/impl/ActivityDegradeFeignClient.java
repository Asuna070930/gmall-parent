package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result<Map<String, Object>> trade() {
        return Result.fail();
    }

    @Override
    public Result getSeckillGoods(String skuId) {
        return Result.fail();
    }

    @Override
    public Result findAll() {
        return Result.fail();
    }
}
