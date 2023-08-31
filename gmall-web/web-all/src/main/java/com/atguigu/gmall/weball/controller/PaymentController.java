package com.atguigu.gmall.weball.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.lient.OrderFeignClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;

@Controller
public class PaymentController {


    @Resource
    private OrderFeignClient orderFeignClient;

    @GetMapping("/pay.html")
    public String toPay(Long orderId, Model model){
        Result<OrderInfo> result = orderFeignClient.getOrderInfo(orderId);
        model.addAttribute("orderInfo",result.getData());
        return "payment/pay";

    }

    /**
     * 成功页面跳转
     * @return
     */
    @GetMapping("/pay/success.html")
    public String success(){

        return "payment/success";
    }

}
