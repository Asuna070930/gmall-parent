package com.atguigu.gmall.cart.client;

import com.atguigu.gmall.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {


    /**
     * /api/cart/getCartCheckedList/{userId}
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId);
}
