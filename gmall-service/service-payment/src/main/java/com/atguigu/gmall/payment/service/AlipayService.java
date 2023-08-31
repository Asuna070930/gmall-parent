package com.atguigu.gmall.payment.service;

public interface AlipayService {
    /**
     * 对接支付-预支付功能
     * @param orderId
     * @return
     */
    String submit(Long orderId);

    /**
     * 退款接口实现
     * @param orderId
     */
    boolean refund(Long orderId);

    /**
     * 查询支付支付记录
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);

    /**
     * 关闭支付支付记录
     * @param orderId
     * @return
     */
    Boolean closePay(Long orderId);

}
