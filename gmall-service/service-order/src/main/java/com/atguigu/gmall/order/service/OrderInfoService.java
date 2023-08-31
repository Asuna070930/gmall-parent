package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface OrderInfoService {


    /**
     * 订单对象转换map
     * @param orderInfo
     * @return
     */
    public Map<String,Object> initWareOrder(OrderInfo orderInfo);

    /**
     * 修改订单状态
     * @param orderId
     * @param processStatus
     */
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus);
    /**
     * 校验库存
     * @param skuId
     * @param num
     * @return
     */
     boolean  checkStock(Long skuId,Integer num);

    /**
     * 删除流水号
     * @param userId
     */
    void deleteTradeNo(String userId);

    /**
     * 校验流水号
     * @param userId
     * @param TradeNo
     * @return
     */
    boolean checkTradeCode(String userId,String TradeNo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 去结算
     * @param userId
     * @return
     */
    Map<String, Object> trade(String userId);

    /**
     * 提交订单
     * @param orderInfo
     * @return
     */
    Long submitOrder(OrderInfo orderInfo);

    /**
     * 我的订单
     * @param userId
     * @param orderInfoPage
     * @return
     */
    IPage<OrderInfo> findOrdersByUserId(String userId, Page<OrderInfo> orderInfoPage);

    /**
     * 根据id查询订单
     * @param orderId
     * @return
     */
    OrderInfo getById(Long orderId);

    /**
     * 订单超时关闭
     * @param orderId
     */
    void execExpiredOrder(Long orderId,String status);

    /**
     * 根据交易订单号查询订单信息
     * @param out_trade_no
     * @return
     */
    OrderInfo getOrderInfoByOutTradeNo(String out_trade_no);

    /**
     * 发送消息扣减库存
     * @param id
     */
    void sendOrderStatus(Long id);

    /**
     *
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap);
}
