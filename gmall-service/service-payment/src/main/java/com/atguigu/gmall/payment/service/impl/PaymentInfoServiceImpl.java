package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.payment.service.PaymentInfoService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

@Service
public class PaymentInfoServiceImpl implements PaymentInfoService {

   @Resource
   private PaymentInfoMapper paymentInfoMapper;
   @Resource
   private RabbitService rabbitService;

   @Autowired
   private RedisTemplate<String,String> redisTemplate;
     /**
     * 保存支付记录
     *
     * @param orderInfo
     */
    @Override
    public void savePaymentInfo(OrderInfo orderInfo) {

        //封装记录信息对象
        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setOrderId(orderInfo.getId());
        paymentInfo.setUserId(orderInfo.getUserId());
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID.name());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setPaymentType(PaymentType.ALIPAY.name());

        //执行保存
        paymentInfoMapper.insert(paymentInfo);
    }

    /**
     * 支付记录查询
     * @param out_trade_no
     * @param name
     * @return
     */
    @Override
    public PaymentInfo getPaymentInfo(String out_trade_no, String name) {

        //select *from payment_info where out_trade_no=? and payment_type=?
        //封装查询条件
        QueryWrapper<PaymentInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("out_trade_no",out_trade_no);
        //判断
        if(!StringUtils.isEmpty(name)){

            queryWrapper.eq("payment_type",name);
        }

        //执行查询
        return paymentInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 修改支付记录为成功状态
     * @param out_trade_no
     * @param name
     * @param paramsMap
     */
    @Override
    public boolean paySuccess(String out_trade_no, String name, Map<String, String> paramsMap) {

        try {



            //封装修改数据对象
            PaymentInfo paymentInfo=new PaymentInfo();
            //设置支付状态
            paymentInfo.setPaymentStatus(PaymentStatus.PAID.name());
            //设置支付宝交易编号
            paymentInfo.setTradeNo(paramsMap.get("trade_no"));
            //设置回调时间
            paymentInfo.setCallbackTime(new Date());
            //设置回调信息
            paymentInfo.setCallbackContent(JSONObject.toJSONString(paramsMap));

            //封装修改条件构造器

//            QueryWrapper<PaymentInfo> queryWrapper=new QueryWrapper<>();
//            queryWrapper.eq("out_trade_no",out_trade_no);
//            queryWrapper.eq("payment_type",name);
//
//            paymentInfoMapper.update(paymentInfo,queryWrapper,paymentInfo);

            this.updatePaymentInfo(out_trade_no,name,paymentInfo);

            //发送更新订单状态的消息
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,
                    MqConst.ROUTING_PAYMENT_PAY,
                    out_trade_no
                    );


            return true;
        } catch (Exception e) {
            //删除redis中notify_id
            redisTemplate.delete(paramsMap.get("notify_id"));
           e.printStackTrace();
        }

        return false;
    }

    /**
     * 订单超时，关闭交易记录
     * @param orderId
     */
    @Override
    public void closedPayment(Long orderId) {
        //封装状态
        PaymentInfo paymentInfo=new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.CLOSED.name());

        //封装修改条件构造器
        QueryWrapper<PaymentInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("order_id",orderId);

        paymentInfoMapper.update(paymentInfo,queryWrapper);

    }

    @Override
    public  void updatePaymentInfo(String out_trade_no, String paymentType,PaymentInfo  paymentInfo){

        //封装修改条件构造器

        QueryWrapper<PaymentInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("out_trade_no",out_trade_no);
        queryWrapper.eq("payment_type",paymentType);

        paymentInfoMapper.update(paymentInfo,queryWrapper);


    }

}
