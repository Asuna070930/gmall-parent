package com.atguigu.gmall.payment.controller;

import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AliPayClientConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;



    /**
     * 关闭支付支付记录
     * @param orderId
     * @return
     */
    @GetMapping("/closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){


        return alipayService.closePay(orderId);
    }

    /**
     * 查询支付支付记录
     * @param orderId
     * @return
     */
    @GetMapping("/checkPaymen/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){


        return alipayService.checkPayment(orderId);
    }




    /**
     * 获取支付记录信息
     * @param outTradeNo
     * @return
     */
    @GetMapping("/getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){

       PaymentInfo paymentInfo= paymentInfoService.getPaymentInfo(outTradeNo,null);

       return paymentInfo;
    }


    /**
     *   //http://localhost:8205/api/payment/alipay/refund/20
     * 退款接口实现
     * @param orderId
     * @return
     */
    @GetMapping("/refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){

        boolean flag=alipayService.refund(orderId);

        //判断
        if(flag){
            return Result.ok();

        }

        return Result.build(null, ResultCodeEnum.FAIL);

    }


    /**
     * 异步回调
     * http://svm5tf.natappfree.cc/api/payment/alipay/callback/notify?name=张三
     *
     * @param paramsMap
     * @return
     */
    @PostMapping("/callback/notify")
    @ResponseBody
    @SneakyThrows
    public String callbackNortify(@RequestParam Map<String, String> paramsMap) {

        //验签
        //调用SDK验证签名
        boolean signVerified = AlipaySignature.rsaCheckV1(paramsMap, AliPayClientConfig.alipay_public_key, AliPayClientConfig.charset, AliPayClientConfig.sign_type);

        if (signVerified) {

            //获取商家交易订单编号
            String out_trade_no = paramsMap.get("out_trade_no");
            //获取支付宝返回的支付金额
            String total_amount = paramsMap.get("total_amount");
            //获取app_id
            String app_id = paramsMap.get("app_id");
            //获取支付记录中支付状态
            String trade_status = paramsMap.get("trade_status");
            //获取notify_id
            String notify_id = paramsMap.get("notify_id");

            //根据out_trade_no查询支付记录--支付宝
            PaymentInfo paymentInfo = paymentInfoService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
            //判断
            if (paymentInfo != null &&
                    new BigDecimal("0.01").compareTo(new BigDecimal(total_amount)) == 0 &&
                    AliPayClientConfig.app_id.equals(app_id)) {
                //判断


                if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {

                    //存储异步回调执行的唯一标识
                    Boolean result = redisTemplate.opsForValue().setIfAbsent(notify_id, notify_id, 1464, TimeUnit.MINUTES);
                    if (result) {

                        //修改支付记录paymentInfo状态为PAID
                        boolean flag = paymentInfoService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramsMap);

                        if (flag) {

                            return "success";
                        }


                    }


                }


                return "failure";
            }


            //响应成功的条件必须是TRADE_SUCCESS 或 TRADE_FINISHED
            return "failure";
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }


    }


    ///api/payment/alipay/callback/return
    @GetMapping("/callback/return")
    public String callbackReturn() {


        //为什么不在此做订单状态更改，库存扣减通知呢？

        //http://payment.gmall.com/pay/success.html
        //重定向到成功界面
        return "redirect:" + AliPayClientConfig.return_order_url;
    }

    /**
     * /api/payment/alipay/submit/{orderId}
     * 对接支付-预支付功能
     *
     * @param orderId
     * @return
     */
    @GetMapping("/submit/{orderId}")
    @ResponseBody
    public String submit(@PathVariable Long orderId) {

        String strHtml = alipayService.submit(orderId);
        return strHtml;

    }
}
