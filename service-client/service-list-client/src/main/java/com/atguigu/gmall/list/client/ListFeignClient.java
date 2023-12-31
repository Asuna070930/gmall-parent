package com.atguigu.gmall.list.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.impl.ListDegradeFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-list",fallback = ListDegradeFeignClient.class)
public interface ListFeignClient {




    /**
     * 商品搜索
     * /api/list
     * @param searchParam
     * @return
     */
    @PostMapping("/api/list")
    public Result search(@RequestBody SearchParam searchParam);
    /**
     *更新商品的热度排名
     * /api/list/inner/incrHotScore/{skuId}
     * @param skuId
     */
    @GetMapping("/api/list/inner/incrHotScore/{skuId}")
    public void  incrHotScore(@PathVariable Long skuId);
}
