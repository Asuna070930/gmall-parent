package com.atguigu.gmall.weball.controller;

import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;

@Controller
public class CartController {


    @Resource
    private ProductFeignClient productFeignClient;

    /**
     * 加入购物车成功后的显示页面
     * @return
     */
    @GetMapping("/addCart.html")
    public String toAddCart(Long skuId, Integer skuNum, Model model){
        //获取skuInfo商品信息回显
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        //设置响应
        model.addAttribute("skuInfo",skuInfo);
        model.addAttribute("skuNum",skuNum);


        return "cart/addCart";
    }


    /**
     * 跳转到购物车列表页面
     * @return
     */
    @GetMapping("/cart.html")
    public  String index(){


        return "cart/index";
    }

}
