package com.atguigu.gmall.order.lient.Impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.lient.OrderFeignClient;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class OrderDegradeFeignClient implements OrderFeignClient {
    @Override
    public Long submitOrder(OrderInfo orderInfo) {
        return null;
    }

    @Override
    public Result<OrderInfo> getOrderInfo(Long orderId) {
        return Result.fail();
    }

    @Override
    public Result trade() {
        return Result.fail();
    }
}
