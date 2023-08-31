package com.atguigu.gmall.payment.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.lient.OrderFeignClient;
import com.atguigu.gmall.payment.config.AliPayClientConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AlipayServiceImpl  implements AlipayService {


    @Resource
    private OrderFeignClient orderFeignClient;

    @Resource
    private PaymentInfoService paymentInfoService;

    @Autowired
    private AlipayClient alipayClient;

    /**
     * 对接支付-预支付功能
     * @param orderId
     * @return
     */
    @Override
    public String submit(Long orderId) {

        //判断订单状态情况
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo!=null && !"UNPAID".equals(orderInfo.getOrderStatus())){

            return "订单状态已关闭或者订单已支付！！！";
        }
        //保存支付记录
        paymentInfoService.savePaymentInfo(orderInfo);
        //对接支付宝
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        //异步接收地址，仅支持http/https，公网可访问
        request.setNotifyUrl(AliPayClientConfig.notify_payment_url);
        //同步跳转地址，仅支持http/https
        //http://api.gmall.com/api/payment/alipay/callback/return
        request.setReturnUrl(AliPayClientConfig.return_payment_url);
        /******必传参数******/
        JSONObject bizContent = new JSONObject();
        //商户订单号，商家自定义，保持唯一性
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        //支付金额，最小值0.01元
        bizContent.put("total_amount", 0.01);
        //订单标题，不可使用特殊符号
        bizContent.put("subject", orderInfo.getTradeBody());
        //电脑网站支付场景固定传值FAST_INSTANT_TRADE_PAY
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        /******可选参数******/
        bizContent.put("timeout_express", "3m");


        request.setBizContent(bizContent.toString());
        AlipayTradePagePayResponse response = null;
        try {
            response = alipayClient.pageExecute(request);
        } catch (AlipayApiException e) {
            throw new RuntimeException(e);
        }

        return response.getBody();
    }

    /**
     * 退款接口实现
     * @param orderId
     */
    @Override
    @SneakyThrows
    public boolean refund(Long orderId) {

        //查询交易记录
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo==null){
            return  false;
        }


        //创建请求对象
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01);

        request.setBizContent(bizContent.toString());
        //执行请求
        AlipayTradeRefundResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            //获取
            String fundChange = response.getFundChange();
            if("Y".equals(fundChange)){
                System.out.println("退款成功");
                //关闭支付记录状态
                PaymentInfo paymentInfo=new PaymentInfo();
                paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());

                paymentInfoService.updatePaymentInfo(orderInfo.getOutTradeNo(), PaymentType.ALIPAY.name(),paymentInfo );

                return true;

            }else{
                System.out.println("退款失败");

            }
        } else {
            System.out.println("调用失败");

        }

        return false;

    }

    /**
     * 查询支付支付记录
     * @param orderId
     * @return
     */
    @Override
    @SneakyThrows
    public Boolean checkPayment(Long orderId) {

        //获取交易号
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo==null){
            return false;
        }


        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{" +
                "  \"out_trade_no\":\""+orderInfo.getOutTradeNo()+"\"," +
//                "  \"trade_no\":\"2014112611001004680 073956707\"," +
                "  \"query_options\":[" +
                "    \"trade_settle_info\"" +
                "  ]" +
                "}");

        AlipayTradeQueryResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }

    /**
     * 关闭支付支付记录
     * @param orderId
     * @return
     */
    @Override
    @SneakyThrows
    public Boolean closePay(Long orderId) {

        //获取交易号
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(orderId).getData();
        //判断
        if(orderInfo==null){
            return false;
        }

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        request.setBizContent(bizContent.toString());
        AlipayTradeCloseResponse response = alipayClient.execute(request);
        if(response.isSuccess()){
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }

    }
}
