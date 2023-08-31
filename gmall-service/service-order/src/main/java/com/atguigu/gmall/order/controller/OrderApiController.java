package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/api/order")
public class OrderApiController {


    @Autowired
    private OrderInfoService orderService;

    @Resource
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private CartFeignClient cartFeignClient;

    @Resource
    private ThreadPoolExecutor executor;




    /**
     * 秒杀下单
     * /api/order/inner/seckill/submitOrder
     * @param orderInfo
     * @return
     */
    @PostMapping("/inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){

        return orderService.submitOrder(orderInfo);
    }



    /**
     * //http://localhost:8204/api/order/orderSplit
     *拆单接口实现
     * @param request
     * @return
     * 87
     * [{"wareId":"1","skuIds":["21","22","24"]},{"wareId":"2","skuIds":["27"]}]
     */
    @PostMapping("/orderSplit")
    public String orderSplit(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        //拆单
       List<OrderInfo> subOrderList= orderService.orderSplit(orderId,wareSkuMap);
       //转换数据类型
        List<Map<String,Object>> mapList=new ArrayList<>();
        //判断
        if(!CollectionUtils.isEmpty(subOrderList)){
            for (OrderInfo orderInfo : subOrderList) {
                //转化对象
                Map<String, Object> orderMap = orderService.initWareOrder(orderInfo);
                mapList.add(orderMap);
            }
        }
        //处理
        return JSONObject.toJSONString(mapList);

    }



    /**
     * /api/order/auth/getOrderInfo/{orderId}
     * 根据id查询订单详情
     * @param orderId
     * @return
     */
    @GetMapping("/auth/getOrderInfo/{orderId}")
    public  Result<OrderInfo> getOrderInfo(@PathVariable Long orderId){

        OrderInfo orderInfo = orderService.getById(orderId);

        return Result.ok(orderInfo);
    }


    /**
     * /api/order/auth/{page}/{limit}
     * 我的订单
     * @param page
     * @param limit
     * @param request
     * @return
     */
    @GetMapping("/auth/{page}/{limit}")
    public Result findOrdersByUserId(@PathVariable Long page,
                                     @PathVariable Long limit ,
                                     HttpServletRequest request){

        //封装分页对象
        Page<OrderInfo> orderInfoPage=new Page<>(page,limit);
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //调用service查询
        IPage<OrderInfo> orderInfoIPage=orderService.findOrdersByUserId(userId,orderInfoPage);

        return Result.ok(orderInfoIPage);

    }


    /**
     * 提交订单
     * /api/order/auth/submitOrder
     *
     * @param orderInfo
     * @return
     */
    @PostMapping("/auth/submitOrder")
    public Result summitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //设置用户id
        orderInfo.setUserId(Long.parseLong(userId));
        //获取流水号
        String tradeNo = request.getParameter("tradeNo");
        //校验流水号
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        //判断
        if (!flag) {

            return Result.fail().message("订单重复提交");
        }

        //定义收集异常的集合
        List<String> errorList=new ArrayList<>();
        //定义集合，收集异步对象
        List<CompletableFuture> futureList=new ArrayList<>();

        for (OrderDetail orderDetail : orderInfo.getOrderDetailList()) {


            CompletableFuture<Void> checkStockFuture = CompletableFuture.runAsync(() -> {

                //校验库存
                //orderDetail具体商品信息
                boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
                //判断
                if (!result) {

                    errorList.add(orderDetail.getSkuId() + " " + orderDetail.getSkuName() + "库存不足");
                }
            }, executor);
            futureList.add(checkStockFuture);


            CompletableFuture<Void> checkPriceFuture = CompletableFuture.runAsync(() -> {

                //校验价格
                BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
                //获取当前订单中商品的价格
                BigDecimal orderPrice = orderDetail.getOrderPrice();
                //比较
                if(orderPrice.compareTo(skuPrice)!=0){

                    //获取所有商品最新价格
                    List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
                    //更新redis  user:1:cart  skuId  cartInfo
                    for (CartInfo cartInfo : cartCheckedList) {
                        redisTemplate.opsForHash().put(RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX,cartInfo.getSkuId().toString(),cartInfo);

                    }


                    errorList.add(orderDetail.getSkuId()+" "+orderDetail.getSkuName()+"价格有变动,请从购物车重新提交！！");

                }


            }, executor);

           futureList.add(checkPriceFuture);


        }

        //异步编排
        CompletableFuture.allOf(
                futureList.toArray(new CompletableFuture[futureList.size()])
        ).join();

        //判断结果
        if(errorList.size()>0){

            return Result.fail().message(StringUtils.join(errorList,","));

        }

        //调用service保存
        Long orderId = orderService.submitOrder(orderInfo);

        //删除流水号
        orderService.deleteTradeNo(userId);

        return Result.ok(orderId);
    }


    /**
     * 去结算
     * api/order/auth/trade
     *
     * @return
     */
    @GetMapping("/auth/trade")
    public Result trade(HttpServletRequest request) {
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //处理整合数据
        Map<String, Object> resultMap = orderService.trade(userId);


        return Result.ok(resultMap);
    }


}
