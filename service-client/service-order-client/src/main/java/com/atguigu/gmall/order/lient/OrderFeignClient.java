package com.atguigu.gmall.order.lient;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.lient.Impl.OrderDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

@FeignClient(value = "service-order",fallback = OrderDegradeFeignClient.class)
public interface OrderFeignClient {

    /**
     * 秒杀下单
     * /api/order/inner/seckill/submitOrder
     * @param orderInfo
     * @return
     */
    @PostMapping("/api/order/inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo);

    /**
     * /api/order/auth/getOrderInfo/{orderId}
     * 根据id查询订单详情
     * @param orderId
     * @return
     */
    @GetMapping("/api/order/auth/getOrderInfo/{orderId}")
    public  Result<OrderInfo> getOrderInfo(@PathVariable Long orderId);
    /**
     * 去结算
     * api/order/auth/trade
     * @return
     */
    @GetMapping("/api/order/auth/trade")
    public Result trade();
}
