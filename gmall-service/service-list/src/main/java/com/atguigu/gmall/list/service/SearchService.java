package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

public interface SearchService {
    /**
     * 商品的上架
     * @param skuId
     */
    void upperGoods(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void lowerGoods(Long skuId);

    /**
     * 更新商品的热度排名
     * @param skuId
     */
    void incrHotScore(Long skuId);

    /**
     * 商品搜索
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam);
}
