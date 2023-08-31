package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.client.payment.PaymentFeignClient;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderInfoService orderInfoService;


    /**
     * 库存扣减结果消费
     * @param mapJson
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true",autoDelete = "false") ,
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER) ,
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    @SneakyThrows
    public void stockProcessResult(String mapJson,Message message ,Channel channel){

        try {
            //判读那
            if(!StringUtils.isEmpty(mapJson)){
                //转换类型
                Map<String,String> stockMap = JSONObject.parseObject(mapJson, Map.class);
                //获取订单id
                String orderId = stockMap.get("orderId");
                //获取状态
                if("DEDUCTED".equals(stockMap.get("status"))){
                    //表示扣减库存成功
                    orderInfoService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
                }else{

                    //库存超卖
                    //方案一：通知其余仓库
                    //方案二：补货 人工客服
                    //设置订单状态
                    orderInfoService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);

                }


            }
        } catch (NumberFormatException e) {
            //通知工作人员，日志系统，运维，对接人
            //记录处理过程
            e.printStackTrace();
        }


        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    /**
     * 订单支付成功更改状态
     * @param out_trade_no
     * @param message
     * @param channel
     */
    @RabbitListener(bindings = @QueueBinding(
            value =@Queue(value =MqConst.QUEUE_PAYMENT_PAY,durable = "true",autoDelete = "false"),
            exchange =@Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key ={MqConst.ROUTING_PAYMENT_PAY}
    ))
    @SneakyThrows
    public void paySuccess(String out_trade_no,Message message,Channel channel){

        try {
            //判断
            if(!StringUtils.isEmpty(out_trade_no)){
                //查询orderInfo
              OrderInfo orderInfo=  orderInfoService.getOrderInfoByOutTradeNo(out_trade_no);
              if(orderInfo!=null){ //此时状态可以不判断
                  //修改订单状态
                  orderInfoService.updateOrderStatus(orderInfo.getId(), ProcessStatus.PAID);
                  //订单信息更新完成，通知库存进行更新（扣减库存）
                  orderInfoService.sendOrderStatus(orderInfo.getId());

              }



            }
        } catch (Exception e) {
            //记录日志，联系管理员。。。。
            e.printStackTrace();
        }



        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);





    }

    @Resource
    private PaymentFeignClient paymentFeignClient;

    /**
     * 订单超时取消订单
     * @param orderId
     * @param message
     * @param channel
     */
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    @SneakyThrows
    public void cancelOrder(Long orderId, Message message, Channel channel){
        closedOrder(orderId);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    private  void closedOrder(Long orderId){

        try {
            //判断
            if(orderId!=null){
                //查询订单
                OrderInfo orderInfo= orderInfoService.getById(orderId);
                //判断
                if(orderInfo!=null && "UNPAID".equals(orderInfo.getOrderStatus())){


                    //判断是否有本地交易记录（用户是否选择支付宝对接，获取二维码）
                    PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                    //判断
                    if(paymentInfo!=null && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                        //表示用户对接支付宝，获取了二维码
                        //查询用户是否扫码了，如果扫码会在支付宝有一条支付记录
                        Boolean flag = paymentFeignClient.checkPayment(orderInfo.getId());
                        //判断
                        if(flag){
                            //表示支付宝有支付记录
                            //执行调用支付宝关闭远程的支付记录
                            Boolean result = paymentFeignClient.closePay(orderInfo.getId());
                            //判断
                            if(result){

                                //关闭订单和本地支付记录
                                orderInfoService.execExpiredOrder(orderId,"2");
                            }else{
                                //网路问题，异常
                                //关闭失败
                                //不用做任何操作，原因是用户支付成功了
                            }


                        }else{
                            //关闭订单和本地支付记录
                            orderInfoService.execExpiredOrder(orderId,"2");

                        }


                    }else{

                        //用户在指定时间内未支付，需要将UNPID改为CLOSED
                        //只关闭订单
                        orderInfoService.execExpiredOrder(orderId,"1");
                    }



                }

            }
        } catch (Exception e) {

            //记录日志，预警，发送邮件短信给运维处理---异常处理--网络问题
            e.printStackTrace();

        }



    }
}
