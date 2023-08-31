package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;

import java.util.List;

public interface BaseCategoryTrademarkService {
    /**
     * 根据category3Id获取品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findTrademarkList(Long category3Id);

    /**
     * 删除分类品牌关联
     * @param category3Id
     * @param trademarkId
     */
    void remove(Long category3Id, Long trademarkId);

    /**
     * 根据category3Id获取可选品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> findCurrentTrademarkList(Long category3Id);

    /**
     * /admin/product/baseCategoryTrademark/save
     * @param categoryTrademarkVo
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);
}
