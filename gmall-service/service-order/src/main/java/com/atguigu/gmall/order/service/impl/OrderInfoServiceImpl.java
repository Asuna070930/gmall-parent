package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.user.client.UserFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class OrderInfoServiceImpl implements OrderInfoService {

    @Resource
    private UserFeignClient userFeignClient;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Resource
    private CartFeignClient cartFeignClient;

    @Resource
    private OrderInfoMapper orderInfoMapper;
    @Resource
    private OrderDetailMapper orderDetailMapper;

    @Value("${ware.url}")
    public String wareUrl;
    /**
     * 校验库存
     * @param skuId
     * @param num
     * @return
     */
    @Override
    public boolean checkStock(Long skuId, Integer num) {
        //定义请求路径
        String url=wareUrl+"/hasStock?skuId="+skuId+"&num="+num;
        //结果是0或者1
        String result = HttpClientUtil.doGet(url);
        //判断
        if(StringUtils.isEmpty(result)){
            return  false;
        }
        return result.equals("1");
    }

    /**
     * 删除流水号
     * @param userId
     */
    @Override
    public void deleteTradeNo(String userId) {
        //获取操作key
        String tradeKey = getTradeKey(userId);
        //删除
        redisTemplate.delete(tradeKey);
    }

    /**
     * 获取流水号的redis的key
     * @param userId
     * @return
     */
    private String getTradeKey(String userId){

        //定义存储key user:1:tradeNo
        String tradeKey= RedisConst.USER_KEY_PREFIX+userId+RedisConst.TRADENO_SUFFIX;
        return tradeKey;
    }
    /**
     * 校验流水号
     * @param userId
     * @param tradeNo
     * @return
     */
    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        //从redis中获取流水号
        //定义存储key user:1:tradeNo
        String tradeKey = getTradeKey(userId);
        String tradeCode = redisTemplate.opsForValue().get(tradeKey);
        //判断
        if(StringUtils.isEmpty(tradeNo)){

            return false;
        }

        return tradeNo.equals(tradeCode);
    }

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    @Override
    public String getTradeNo(String userId) {
        //生成流水号
        String tradeNo = UUID.randomUUID().toString().replaceAll("-", "");
        //定义存储key user:1:tradeNo
        String tradeKey = getTradeKey(userId);
        //存储redis
        redisTemplate.opsForValue().set(tradeKey,tradeNo,RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        return tradeNo;
    }

    /**
     * 去结算
     * @param userId
     * @return
     */
    @Override
    public Map<String, Object> trade(String userId) {
        //定义封装对象
        Map<String, Object> resultMap=new HashMap<>();
        //获取用户地址列表
        List<UserAddress> userAddressListByUserId = userFeignClient.findUserAddressListByUserId(Long.valueOf(userId));
        resultMap.put("userAddressList",userAddressListByUserId);
        //获取用户送货清单
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //转化数据
         if(!CollectionUtils.isEmpty(cartCheckedList)){
             //计算商品数量
             AtomicReference<Integer> count= new AtomicReference<>(0);

             List<OrderDetail> orderDetailList = cartCheckedList.stream().map(cartInfo -> {
                 //创建订单项
                 OrderDetail orderDetail = new OrderDetail();
                 orderDetail.setSkuId(cartInfo.getSkuId());
                 orderDetail.setSkuName(cartInfo.getSkuName());
                 orderDetail.setImgUrl(cartInfo.getImgUrl());
                 orderDetail.setSkuNum(cartInfo.getSkuNum());
                 orderDetail.setOrderPrice(cartInfo.getSkuPrice());
                 //计算
                 count.updateAndGet(v -> v + cartInfo.getSkuNum());


                 return orderDetail;
             }).collect(Collectors.toList());
             resultMap.put("detailArrayList",orderDetailList);

             //计算总金额
             OrderInfo orderInfo=new OrderInfo();
             orderInfo.setOrderDetailList(orderDetailList);
             //调用计算方法
             orderInfo.sumTotalAmount();
             resultMap.put("totalAmount",orderInfo.getTotalAmount());

             //计算数量
             resultMap.put("totalNum",count.get());



             resultMap.put("tradeNo",getTradeNo(userId));



         }


        return resultMap;
    }

    @Resource
    private RabbitService rabbitService;
    /**
     * 提交订单
     * @param orderInfo
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrder(OrderInfo orderInfo) {

        //补充orderInfo数据
        //总金额
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //订单交易编号--支付宝对接
        String outTradeNo = "ATGUIGU" + System.currentTimeMillis() + "" + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //订单描述
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //定义字符串拼接对象
        StringBuilder builder=new StringBuilder();
        for (OrderDetail orderDetail : orderDetailList) {
            builder.append(orderDetail.getSkuName()+"  ");

        }
        //判断长度
        if(builder.length()>=50){

            orderInfo.setTradeBody( builder.toString().substring(0,50));

        }else{
            orderInfo.setTradeBody( builder.toString());

        }
        //操作时间
        orderInfo.setOperateTime(new Date());
        //失效时间--一天
        Calendar calendar = Calendar.getInstance();
        //加一天
        calendar.add(Calendar.DATE,1);

        //获取时间对象
        Date time = calendar.getTime();

        orderInfo.setExpireTime(time);
        //进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());

        //保存order_info
        orderInfoMapper.insert(orderInfo);




        //保存order_detail
        for (OrderDetail orderDetail : orderDetailList) {
            //关联订单id
            orderDetail.setOrderId(orderInfo.getId());

            orderDetailMapper.insert(orderDetail);
        }


        //发送消息
        rabbitService.sendDelayMessage(
                MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,
                MqConst.ROUTING_ORDER_CANCEL,
                orderInfo.getId(),
                MqConst.DELAY_TIME
        );
        //返回订单id

        return orderInfo.getId();
    }

    /**
     * 我的订单
     * @param userId
     * @param orderInfoPage
     * @return
     */
    @Override
    public IPage<OrderInfo> findOrdersByUserId(String userId, Page<OrderInfo> orderInfoPage) {
        //调用mapper处理
       IPage<OrderInfo> infoIPage= orderInfoMapper.selectOrdersByUserId(userId,orderInfoPage);

       //将orderStatusName赋值
        for (OrderInfo record : infoIPage.getRecords()) {

            String orderStatus = record.getOrderStatus();//UNPAID
            record.setOrderStatusName(OrderStatus.getStatusNameByStatus(orderStatus));
        }



        return infoIPage;
    }

    /**
     * 根据id查询订单
     * @param orderId
     * @return
     */
    @Override
    public OrderInfo getById(Long orderId) {
        //查询订单信息
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //查询订单明细列表
        QueryWrapper<OrderDetail> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("order_id",orderId);

        List<OrderDetail> orderDetails = orderDetailMapper.selectList(queryWrapper);

        //设置订单明细列表到订单中
        if(orderInfo!=null){
            orderInfo.setOrderDetailList(orderDetails);
        }
        return orderInfo;
    }

    /**
     * 关闭订单
     * @param orderId
     */
    @Override
    public void execExpiredOrder(Long orderId,String status) {


      updateOrderStatus(orderId,ProcessStatus.CLOSED);

      //判断
        if("2".equals(status)){
            //发送消息删除本地支付记录
            rabbitService.sendMessage(
                    MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,
                    MqConst.ROUTING_PAYMENT_CLOSE,
                    orderId
            );
        }


    }

    /**
     *  根据交易订单号查询订单信息
     * @param out_trade_no
     * @return
     */
    @Override
    public OrderInfo getOrderInfoByOutTradeNo(String out_trade_no) {

        QueryWrapper<OrderInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("out_trade_no",out_trade_no);

        return orderInfoMapper.selectOne(queryWrapper);
    }

    /**
     * 发送消息扣减库存
     * @param
     */
    @Override
    public void sendOrderStatus(Long orderId) {

        //更新订单状态 --NOTIFIED_WARE
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);

        //整合数据 --OrderInfo--JSON
        String wareJson=initWareOrder(orderId);
        //确定消息队列绑定关系，发送消息
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_WARE_STOCK,
                MqConst.ROUTING_WARE_STOCK,
                wareJson);

    }

//    public static void main(String[] args) {
//
//        String wreSku="[{\"wareId\":\"1\",\"skuIds\":[\"21\",\"22\",\"24\"]},{\"wareId\":\"2\",\"skuIds\":[\"27\"]}]";
//        List<Map> mapList = JSONObject.parseArray(wreSku, Map.class);
//        for (Map map : mapList) {
//            List<String> skuIds = (List<String>) map.get("skuIds");
//
//            System.out.println(skuIds);
//        }
//
//    }
    /**
     * 拆单实现
     * @param orderId
     * @param wareSkuMap
     * @return
     *
     * 拆单规则
     * [{"wareId":"1","skuIds":["21","22","24"]},{"wareId":"2","skuIds":["27"]}]
     *
     */
    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {

        //定义集合收集子订单
        List<OrderInfo> subOrderList=new ArrayList<>();
        //获取原始订单
        OrderInfo originOrderInfo = getById(Long.parseLong(orderId));
        //转换拆单规则
        List<Map> mapList = JSONObject.parseArray(wareSkuMap, Map.class);
        //遍历
        for (Map map : mapList) {
            //获取仓库id
            String wareId = (String) map.get("wareId");
            // {"wareId":"1","skuIds":["21","22","24"]}
            //创建子订单
            OrderInfo subOrderInfo=new OrderInfo();
            //拷贝数据
            BeanUtils.copyProperties(originOrderInfo,subOrderInfo);
            //Id置空
            subOrderInfo.setId(null);
            //仓库字段
            subOrderInfo.setWareId(wareId);
            //获取父订单所有商品明细
            List<OrderDetail> orderDetailList = originOrderInfo.getOrderDetailList();
            //定义子订单明细集合
            List<OrderDetail> subOrderDetailList=new ArrayList<>();

            //获取拆分的skuid的规则"skuIds":["21","22","24"]
            List<String> skuIds = (List<String>) map.get("skuIds");

            //获取当前子订单的商品
            if(!CollectionUtils.isEmpty(orderDetailList)){

                for (OrderDetail orderDetail : orderDetailList) {

                    for (String skuId : skuIds) {

                        if(orderDetail.getSkuId().toString().equals(skuId)){
                            //收集符合的子订单明细
                            subOrderDetailList.add(orderDetail);

                        }
                    }

                }

            }

            //设置订单明细
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            //总金额更新--订单明细
            subOrderInfo.sumTotalAmount();

            //定义字符串拼接对象
            StringBuilder builder=new StringBuilder();
            //订单概述--订单明细
            for (OrderDetail orderDetail : subOrderDetailList) {

                builder.append(orderDetail.getSkuName()).append(",");
            }
            //判断
            if(builder.toString().length()>100){

                subOrderInfo.setTradeBody(builder.toString().substring(0, 100));
            }

            subOrderInfo.setTradeBody(builder.toString());



            //保存订单
            orderInfoMapper.insert(subOrderInfo);
            //保存明细
            for (OrderDetail orderDetail : subOrderDetailList) {

                //设置订单id
                orderDetail.setOrderId(subOrderInfo.getId());

                orderDetailMapper.insert(orderDetail);

            }

            //修改父订单状态
            updateOrderStatus(originOrderInfo.getId(),ProcessStatus.SPLIT);

            //收集拆分的子订单
            subOrderList.add(subOrderInfo);

        }

        return subOrderList;
    }


    /**
     * 根据id查询订单，转换成json数据
     * @param orderId
     * @return
     */
    private String initWareOrder(Long orderId) {
        //根据id查询订单对象
        OrderInfo orderInfo = getById(orderId);
        //转换订单对象类型map
        Map<String,Object> resultMap=this.initWareOrder(orderInfo);

        //转换json字符串
        return JSONObject.toJSONString(resultMap);
    }

    /**
     * 转化orderInfo为map对象，符合库存系统的json数据结构
     * @param orderInfo
     * @return
     */
    @Override
    public Map<String,Object> initWareOrder(OrderInfo orderInfo){
        //封装转换对象
        Map<String,Object> resultMap=new HashMap<>();
        //设置数据
        resultMap.put("orderId",orderInfo.getId());
        resultMap.put("consignee",orderInfo.getConsignee());
        resultMap.put("consigneeTel",orderInfo.getConsigneeTel());
        resultMap.put("orderComment",orderInfo.getOrderComment());
        resultMap.put("orderBody",orderInfo.getTradeBody());
        resultMap.put("deliveryAddress",orderInfo.getDeliveryAddress());
//        resultMap.put("paymentWay","ONLINE".equals(orderInfo.getPaymentWay())?2:1);
        resultMap.put("paymentWay","2");
        resultMap.put("wareId",orderInfo.getWareId());

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        //遍历处理
        List<Map<String, Object>> mapList = orderDetailList.stream().map(orderDetail -> {
            //定义封装对象
            Map<String, Object> detail = new HashMap<>();
            detail.put("skuId", orderDetail.getSkuId());
            detail.put("skuNum", orderDetail.getSkuNum());
            detail.put("skuName", orderDetail.getSkuName());

            return detail;

        }).collect(Collectors.toList());


        resultMap.put("details",mapList);

        return resultMap;
    }

    /**
     * 修改订单
     * @param orderId  订单id
     * @param processStatus  订单状态
     */
    @Override
    public void updateOrderStatus(Long orderId,ProcessStatus processStatus){
        //创建修改内容
        OrderInfo orderInfo=new OrderInfo();

        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        //设置条件对象
        QueryWrapper<OrderInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("id",orderId);


        //执行修改
        orderInfoMapper.update(orderInfo,queryWrapper);



    }




}
