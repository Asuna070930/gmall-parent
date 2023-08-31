package com.atguigu.gmall.cart.service.impl;

import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartInfoServiceImpl implements CartInfoService {


    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 加入购物车
     * @param userId
     * @param skuId
     * @param skuNum
     *
     * redis---hash
     *  key---userId
     *  value ---skuId
     *        ---cartInfo
     *
     *
     * 添加的思路；
     *   1.商品添加时，第一次构建该商品的存储结构
     *
     *   2.商品添加时，对同一个商品进行数量相加即可
     *
     *
     */
    @Override
    public void addToCart(String userId, Long skuId, Integer skuNum) {

        //创建存储key
        String cartKey=this.getCartkey(userId);

        //获取该商品数据
        CartInfo cartInfo=null;
        //获取当前添加商品的购物车项
        BoundHashOperations boundHashOps = redisTemplate.boundHashOps(cartKey);
        //判断
        if(boundHashOps.hasKey(skuId.toString())){//已经有该商品的购物项，第二次添加该商品

            //获取该商品数据
             cartInfo = (CartInfo) boundHashOps.get(skuId.toString());
            //数量相加
            cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
            //更新时间
            cartInfo.setUpdateTime(new Date());
            //更新价格
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(skuId));

        }else{
            //获取该商品的数据信息
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);

            //没有对应的购物车，第一次添加该商品
           cartInfo= new CartInfo();
            cartInfo.setUserId(userId);
            cartInfo.setSkuId(skuId);
            //加入的的价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuName(skuInfo.getSkuName());
            //实时价格
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCreateTime(new Date());
            cartInfo.setUpdateTime(new Date());


        }
        //存储
        boundHashOps.put(skuId.toString(),cartInfo);
        boundHashOps.expire(RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }

    /**
     * 购物车列表--合并购物车
     * @param userId
     * @param userTempId
     * @return
     */
    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {

        //定义未登录购物车
        List<CartInfo> noLoginCartList=new ArrayList<>();

        //获取未登录购物车
        if(!StringUtils.isEmpty(userTempId)){

            //获取cartkey
            String cartkey = getCartkey(userTempId);
            //获取数据
            noLoginCartList = redisTemplate.boundHashOps(cartkey).values();

        }


        //用户是否登录
        if(StringUtils.isEmpty(userId)){

            //判断
            if(!CollectionUtils.isEmpty(noLoginCartList)){

                noLoginCartList.sort((o1,o2)->{

                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(),Calendar.SECOND);
                });
            }

            return  noLoginCartList;
        }


        //此处表示用户已经登录
        //获取用户登录后的购物车操作对象
        String cartkey = getCartkey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartkey);
        //判断是否合并
        if(!CollectionUtils.isEmpty(noLoginCartList)){

            //遍历 未登录的购物项--cartInfo
            for (CartInfo cartInfo : noLoginCartList) {

                if(boundHashOperations.hasKey(cartInfo.getSkuId().toString())){
                    //相同购物项
                    CartInfo loginCartInfo = (CartInfo) boundHashOperations.get(cartInfo.getSkuId().toString());
                    //更新数量
                    loginCartInfo.setSkuNum(loginCartInfo.getSkuNum()+cartInfo.getSkuNum());
                    //更新时间
                    loginCartInfo.setUpdateTime(new Date());
                    //更新价格
                    loginCartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
                    //选中
                    /// 未登录没选中  ，登录没有选中
                    //选中
                    if(cartInfo.getIsChecked()==1){

                        loginCartInfo.setIsChecked(1);
                    }
                    //更新
                    boundHashOperations.put(cartInfo.getSkuId().toString(),loginCartInfo);

                }else{
                    //没有相同购物项
                    //设置用户id
                    cartInfo.setUserId(userId);
                    //设置时间
                    cartInfo.setCreateTime(new Date());
                    cartInfo.setUpdateTime(new Date());

                    boundHashOperations.put(cartInfo.getSkuId().toString(),cartInfo);



                }



            }


            //合并完成--删除临时
            redisTemplate.delete(getCartkey(userTempId));


        }

        //获取登录后的购物车
        List<CartInfo> loginCartList = boundHashOperations.values();
        //判断
        if(CollectionUtils.isEmpty(loginCartList)){

            return new ArrayList<>();
        }

        //排序
        loginCartList.sort(new Comparator<CartInfo>() {
            @Override
            public int compare(CartInfo o1, CartInfo o2) {
                return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(),Calendar.SECOND);
            }
        });


        return loginCartList;
    }

    /**
     * 选中状态变更
     * @param skuId
     * @param isCheck
     * @param userId
     */
    @Override
    public void checkCart(String skuId, Integer isCheck, String userId) {

        //获取key
        String cartkey = getCartkey(userId);
        //获取redis数据
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartkey);
        //获取对象数据
        CartInfo cartInfo = (CartInfo) boundHashOperations.get(skuId);
        //设置
        cartInfo.setIsChecked(isCheck);
        //更新
        boundHashOperations.put(skuId,cartInfo);



    }

    /**
     *  删除购物项
     * @param skuId
     * @param userId
     */
    @Override
    public void deleteCart(String skuId, String userId) {

        //删除
        redisTemplate.boundHashOps(getCartkey(userId)).delete(skuId);


    }

    /**
     * 删除选中的购物项
     * @param userId
     */
    @Override
    public void deleteChecked(String userId) {
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(getCartkey(userId));
        //所有选中的id
        List<CartInfo> cartInfoList = boundHashOperations.values();
        //判断
        if(!CollectionUtils.isEmpty(cartInfoList)){

            for (CartInfo cartInfo : cartInfoList) {
                //判断
                if(cartInfo.getIsChecked().intValue()==1){
                    //删除
                    boundHashOperations.delete(cartInfo.getSkuId().toString());


                }

            }


        }





    }

    /**
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        //获取购物车的key
        String cartkey = getCartkey(userId);
        //获取用户购物车
        List<CartInfo> cartInfoList = redisTemplate.boundHashOps(cartkey).values();

        List<CartInfo> checkCartInfoList = cartInfoList.stream().filter(cartInfo -> {
            //确认商品的价格
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            return cartInfo.getIsChecked().intValue() == 1;
        }).collect(Collectors.toList());


        return checkCartInfoList;
    }

//    /**
//     * 购物车列表
//     * @param userId
//     * @param userTempId
//     * @return
//     */
//    @Override
//    public List<CartInfo> getCartList(String userId, String userTempId) {
//
//        //定义接收数据的集合
//        List<CartInfo> cartInfoList=null;
//
//        //判断
//        if(!StringUtils.isEmpty(userId)){
//            //获取cartkey
//            String cartkey = getCartkey(userId);
//            //获取数据
//           cartInfoList = redisTemplate.boundHashOps(cartkey).values();
//
//
//        }
//        //判断userTempId
//        if(!StringUtils.isEmpty(userTempId)){
//            //获取cartKey
//            String cartkey = getCartkey(userTempId);
//            //获取数据
//            cartInfoList = redisTemplate.boundHashOps(cartkey).values();
//
//        }
//
//
//        //排序
//        if(!CollectionUtils.isEmpty(cartInfoList)){
//            //排序
//            cartInfoList.sort(new Comparator<CartInfo>() {
//                @Override
//                public int compare(CartInfo o1, CartInfo o2) {
//
//                    return DateUtil.truncatedCompareTo(o2.getUpdateTime(),o1.getUpdateTime(), Calendar.SECOND);
//                }
//            });
//
//
//        }
//
//
//
//
//        return cartInfoList;
//    }

    /**
     * 购车车的key
     * @param userId
     * @return
     * user:1:cart
     */
    private String getCartkey(String userId) {

        return RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
