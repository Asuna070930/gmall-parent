package com.atguigu.gmall.client.payment;

import com.atguigu.gmall.client.payment.impl.PaymentDegradeFeignClient;
import com.atguigu.gmall.model.payment.PaymentInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@FeignClient(value = "service-payment",fallback = PaymentDegradeFeignClient.class)
public interface PaymentFeignClient {
    /**
     * 关闭支付支付记录
     * @param orderId
     * @return
     */
    @GetMapping("/api/payment/alipay/closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId);

    /**
     * 查询支付支付记录
     * @param orderId
     * @return
     */
    @GetMapping("/api/payment/alipay/checkPaymen/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId);




    /**
     * 获取支付记录信息
     * @param outTradeNo
     * @return
     */
    @GetMapping("/api/payment/alipay/getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo);



}
