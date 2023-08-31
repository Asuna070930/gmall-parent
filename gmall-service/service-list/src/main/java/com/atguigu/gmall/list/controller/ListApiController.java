package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.bind.annotation.*;

import javax.jws.Oneway;

@RestController
@RequestMapping("/api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;


    /**
     * 商品搜索
     * /api/list
     * @param searchParam
     * @return
     */
    @PostMapping
    public Result search(@RequestBody SearchParam searchParam){

        SearchResponseVo searchResponseVo=searchService.search(searchParam);

        return Result.ok(searchResponseVo);
    }

    /**
     *更新商品的热度排名
     * /api/list/inner/incrHotScore/{skuId}
     * @param skuId
     */
    @GetMapping("/inner/incrHotScore/{skuId}")
    public void  incrHotScore(@PathVariable Long skuId){

        searchService.incrHotScore(skuId);


    }

    /**
     * /api/list/inner/lowerGoods/{skuId}
     * 商品下架
     * @param skuId
     * @return
     */
    @GetMapping("/inner/lowerGoods/{skuId}")
    public  Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    /**
     * api/list/inner/upperGoods/{skuId}
     * 商品的上架
     */
    @GetMapping("/inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);


        return Result.ok();
    }

    /**
     *
     * /api/list/createIndex
     * 创建索引库和mapping映射
     * @return
     */
    @GetMapping("/createIndex")
    public Result  createIndex(){

        //创建索引库
        restTemplate.createIndex(Goods.class);
        //创建映射关系
        restTemplate.putMapping(Goods.class);


        return Result.ok();
    }
}
