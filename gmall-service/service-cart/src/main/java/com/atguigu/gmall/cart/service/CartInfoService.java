package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartInfoService {
    /**
     * cartInfoService
     * @param userId
     * @param skuId
     * @param skuNum
     */
    void addToCart(String userId, Long skuId, Integer skuNum);

    /**
     * 购物车列表
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 选中状态变更
     * @param skuId
     * @param isCheck
     * @param userId
     */
    void checkCart(String skuId, Integer isCheck, String userId);

    /**
     *  删除购物项
     * @param skuId
     * @param userId
     */
    void deleteCart(String skuId, String userId);

    /**
     * 删除选中的购物项
     * @param userId
     */
    void deleteChecked(String userId);

    /**
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);
}
