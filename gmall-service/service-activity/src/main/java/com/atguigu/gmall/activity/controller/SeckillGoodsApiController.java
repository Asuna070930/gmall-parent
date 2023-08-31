package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.lient.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.xml.crypto.Data;
import java.util.*;

@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsApiController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private OrderFeignClient orderFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 秒杀下单
     * api/activity/seckill/auth/submitOrder
     *
     * @return
     */
    @PostMapping("/auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        //调用service-order
        Long orderId = orderFeignClient.submitOrder(orderInfo);
        //判断
        if (orderId != null) {
            //保存用户秒订单
            redisTemplate.opsForHash().put(RedisConst.SECKILL_ORDERS_USERS,userId,orderId);

            //清除临时订单
            redisTemplate.opsForHash().delete(RedisConst.SECKILL_ORDERS,userId);

            return Result.ok(orderId);
        }

        return Result.fail();
    }


    /**
     * /api/activity/seckill/auth/trade
     *
     * @return
     */
    @GetMapping("/auth/trade")
    public Result<Map<String, Object>> trade(HttpServletRequest request) {
        Map<String, Object> resultMap = new HashMap<>();
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //查询秒杀的临时订单
        OrderRecode recode = (OrderRecode) redisTemplate.opsForHash().get(RedisConst.SECKILL_ORDERS, userId);
        //判断
        if (recode == null) {
            return Result.build(null, ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //查询用户的地址列表
        List<UserAddress> addresses = userFeignClient.findUserAddressListByUserId(Long.parseLong(userId));
        resultMap.put("userAddressList", addresses);
        //转换成订单详情
        SeckillGoods seckillGoods = recode.getSeckillGoods();
        //定义接收的集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        //创建订单详情
        OrderDetail detail = new OrderDetail();
        detail.setSkuId(seckillGoods.getSkuId());
        detail.setSkuName(seckillGoods.getSkuName());
        detail.setImgUrl(seckillGoods.getSkuDefaultImg());
        detail.setOrderPrice(seckillGoods.getCostPrice());
        detail.setSkuNum(1);
        orderDetailList.add(detail);
        resultMap.put("detailArrayList", orderDetailList);

        //计算总金额
        resultMap.put("totalAmount", seckillGoods.getCostPrice());
        //数量
        resultMap.put("totalNum", "1");

        return Result.ok(resultMap);
    }


    /**
     * api/activity/seckill/auth/checkOrder/23
     * 轮询查询抢购结果
     *
     * @param skuId
     * @return
     */
    @GetMapping("/auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable String skuId, HttpServletRequest request) {
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        return seckillGoodsService.checkOrder(skuId, userId);
    }


    /**
     * 秒杀抢购--加入mq
     * api/activity/seckill/auth/seckillOrder/23?skuIdStr=202cb962ac59075b964b07152d234b70
     *
     * @param skuId
     * @param skuIdStr
     * @return
     */
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable String skuId, String skuIdStr, HttpServletRequest request) {


        //获取userId
        String userId = AuthContextHolder.getUserId(request);

        return seckillGoodsService.seckillOrder(skuId, skuIdStr, userId);
    }

    /**
     * 获取下单码
     * /api/activity/seckill/auth/getSeckillSkuIdStr/{skuId}
     * /api/activity/seckill/auth/getSeckillSkuIdStr/23
     *
     * @return
     */
    @GetMapping("/auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request) {

        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //查询该用户抢购的商品信息
        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId.toString());
        //判断是否进入了秒杀时段
        //获取当期时间
        Date current = new Date();
        //判断
        if (DateUtil.dateCompare(seckillGoods.getStartTime(), current) && DateUtil.dateCompare(current, seckillGoods.getEndTime())) {
            //生成抢购码
            String encrypt = MD5.encrypt(userId + skuId);

            return Result.ok(encrypt);
        }


        return Result.fail(ResultCodeEnum.ILLEGAL_REQUEST);

    }


    /**
     * ///api/activity/seckill/getSeckillGoods/{skuId}
     * 秒杀详情页数据查询
     *
     * @param skuId
     * @return
     */
    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable String skuId) {

        SeckillGoods seckillGoods = seckillGoodsService.getSeckillGoods(skuId);

        return Result.ok(seckillGoods);
    }


    /**
     * /api/activity/seckill/findAll
     * 查询秒杀列表数据
     *
     * @return
     */
    @GetMapping("/findAll")
    public Result findAll() {

        List<SeckillGoods> seckillGoods = seckillGoodsService.findAll();


        return Result.ok(seckillGoods);
    }
}
