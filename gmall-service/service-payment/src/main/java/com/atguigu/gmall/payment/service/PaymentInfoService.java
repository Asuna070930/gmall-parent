package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

public interface PaymentInfoService {

    /**
     * 修改支付记录状态
     * @param out_trade_no
     * @param paymentType
     * @param paymentInfo
     */
    public  void updatePaymentInfo(String out_trade_no, String paymentType,PaymentInfo  paymentInfo);
    /**
     * 保存支付记录
     * @param orderInfo
     */
    void savePaymentInfo(OrderInfo orderInfo);

    /**
     * 查询支付记录
     * @param out_trade_no
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no, String name);

    /**
     * 修改支付记录状态
     * @param out_trade_no
     * @param name
     * @param paramsMap
     */
    boolean paySuccess(String out_trade_no, String name, Map<String, String> paramsMap);

    /**
     * 订单超时，关闭交易记录
     * @param orderId
     */
    void closedPayment(Long orderId);
}
