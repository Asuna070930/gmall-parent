package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ItemServiceImpl  implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ListFeignClient listFeignClient;

    @Autowired
    private ThreadPoolExecutor executor;
    /**
     * 查询商品详情
     * 整合详情页需要的数据，共7个接口
     * @param skuId
     * @return
     */
    @Override
    public Map<String, Object> getItem(Long skuId) {
        Map<String, Object> resultMap=new HashMap<>();

//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        //判断当前请求的skuId是否存在
//        if(!bloomFilter.contains(skuId)){
//
//            return resultMap;
//        }


        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {

            //获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            resultMap.put("skuInfo",skuInfo);

            return skuInfo;
        },executor);


        CompletableFuture<Void> categoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取三级分类
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            resultMap.put("categoryView", categoryView);
        }, executor);


        CompletableFuture<Void> findSpuPosterBySpuIdCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取商品海报
            List<SpuPoster> spuPosterBySpuId = productFeignClient.findSpuPosterBySpuId(skuInfo.getSpuId());
            resultMap.put("spuPosterList", spuPosterBySpuId);
        }, executor);


        CompletableFuture<Void> spuSaleAttrListCheckBySkuCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取销售属性销售属性值及选中关系
            List<SpuSaleAttr> spuSaleAttrListCheckBySku = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            resultMap.put("spuSaleAttrList", spuSaleAttrListCheckBySku);

        }, executor);


        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //获取sku的切换关系
            Map<String, String> skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            resultMap.put("valuesSkuJson", JSON.toJSONString(skuValueIdsMap));
        }, executor);


        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {

            //获取实时价格
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);

            resultMap.put("price", skuPrice);


        }, executor);

        CompletableFuture<Void> attrListCompletableFuture = CompletableFuture.runAsync(() -> {
            //获取平台属性
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);
            //处理平台属性

            List<Map<String, String>> attrListMap = attrList.stream().map(baseAttrInfo -> {
                Map<String, String> attrMap = new HashMap<>();
                attrMap.put("attrName", baseAttrInfo.getAttrName());
                attrMap.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());


                return attrMap;
            }).collect(Collectors.toList());

            resultMap.put("skuAttrList", attrListMap);


        }, executor);

        //统计热度
        CompletableFuture<Void> incrHotscoreCompletable = CompletableFuture.runAsync(() -> {

            listFeignClient.incrHotScore(skuId);

        }, executor);


        //所有任务完成后才返回结果
        CompletableFuture.allOf(
                skuValueIdsMapCompletableFuture,
                categoryViewCompletableFuture,
                findSpuPosterBySpuIdCompletableFuture,
                skuPriceCompletableFuture,
                skuInfoCompletableFuture,
                spuSaleAttrListCheckBySkuCompletableFuture,
                attrListCompletableFuture,
                incrHotscoreCompletable
        ).join();

        return resultMap;
    }
}
