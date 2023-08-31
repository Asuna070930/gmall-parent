package com.atguigu.gmall.weball.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.lient.OrderFeignClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.Mapping;

import javax.annotation.Resource;
import java.util.Map;

@Controller
public class OrderController {


    @Resource
    private OrderFeignClient orderFeignClient;


    /**
     * 我的订单
     * @return
     */
    @GetMapping("/myOrder.html")
    public String myOrder(){

        return "order/myOrder";
    }
    /**
     * 结算页数据显示
     * @return
     */
    @GetMapping("/trade.html")
    public String trade(Model model){

        //获取结算页数据
        Result<Map<String,Object>> trade = orderFeignClient.trade();

        //响应数据
        model.addAllAttributes(trade.getData());


        return "order/trade";
    }
}
