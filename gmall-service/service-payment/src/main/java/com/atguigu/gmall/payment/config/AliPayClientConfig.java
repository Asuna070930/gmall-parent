package com.atguigu.gmall.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AliPayClientConfig {



    public static String notify_payment_url;

    @Value("${notify_payment_url}")
    public void notifyPaymentUrl(String notify_payment_url){

        this.notify_payment_url=notify_payment_url;
    }

    public static String return_order_url;

    @Value("${return_order_url}")
    public void returnOrderurl(String return_order_url){

        this.return_order_url=return_order_url;
    }

    public static String return_payment_url;

    @Value("${return_payment_url}")
    public void returnPaymentUrl(String return_payment_url){

        this.return_payment_url=return_payment_url;
    }


    @Value("${alipay_url}")
    private String alipay_url;

    @Value("${app_private_key}")
    private String app_private_key;

    public  static String app_id;

    @Value("${app_id}")
    public void appId(String app_id){

        this.app_id=app_id;
    }


    public static String alipay_public_key;


    @Value("${alipay_public_key}")
    public void alipayPublicKey(String alipay_public_key){

        this.alipay_public_key=alipay_public_key;

    }


    public final static String format="json";
    public final static String charset="utf-8";
    public final static String sign_type="RSA2";




    @Bean
    public AlipayClient alipayClient(){

        AlipayClient alipayClient =
                new DefaultAlipayClient(
                        alipay_url,
                        app_id,
                        app_private_key,
                        format,
                        charset,
                        alipay_public_key,
                        sign_type);


        return alipayClient;
    }
}
