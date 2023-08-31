package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@FeignClient(value = "service-activity")
public interface ActivityFeignClient {

    /**
     * /api/activity/seckill/auth/trade
     * @return
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    public Result<Map<String,Object>> trade();

    /**
     *  ///api/activity/seckill/getSeckillGoods/{skuId}
     * 秒杀详情页数据查询
     * @param skuId
     * @return
     */
    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable String skuId);
    /**
     * /api/activity/seckill/findAll
     * 查询秒杀列表数据
     * @return
     */
    @GetMapping("/api/activity/seckill/findAll")
    public Result findAll();
}
