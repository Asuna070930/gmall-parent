package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartInfoService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.cart.CartInfoVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import java.util.List;

@RestController
@RequestMapping("/api/cart/")
public class CartApiController {


    @Autowired
    private CartInfoService cartInfoService;




    /**
     * /api/cart/getCartCheckedList/{userId}
     * 获取选中状态的购物车列表
     * @param userId
     * @return
     */
    @GetMapping("/getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){

        return  cartInfoService.getCartCheckedList(userId);
    }

    /**
     *
     * /api/cart/deleteChecked
     * 删除选中的购物项
     * @param request
     * @return
     */
    @DeleteMapping("/deleteChecked")
    public Result deleteChecked(HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //判断
        if(StringUtils.isEmpty(userId)){
            userId= AuthContextHolder.getUserTempId(request);
        }

        cartInfoService.deleteChecked(userId);

        return Result.ok();

    }


    /**
     * /api/cart/deleteCart/28
     * 删除购物项
     * @param skuId
     * @param request
     * @return
     */
    @DeleteMapping("/deleteCart/{skuId}")
    public  Result deleteCart(@PathVariable String skuId,HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //判断
        if(StringUtils.isEmpty(userId)){
            userId= AuthContextHolder.getUserTempId(request);
        }

        cartInfoService.deleteCart(skuId,userId);
        return Result.ok();
    }


    /**
     * 选中状态变更
     * @param skuId
     * @param isCheck
     * @return
     */
    @GetMapping("/checkCart/{skuId}/{isCheck}")
    public Result checkCart(@PathVariable String skuId,
                            @PathVariable Integer isCheck,
                            HttpServletRequest request){
        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //判断
        if(StringUtils.isEmpty(userId)){
            userId= AuthContextHolder.getUserTempId(request);
        }

        cartInfoService.checkCart(skuId,isCheck,userId);

        return Result.ok();
    }



    /**
     *
     * /api/cart/cartList
     * 购物车列表
     * @return
     */
    @GetMapping("/cartList")
    public Result cartList(HttpServletRequest request){

        //获取userId
        String userId = AuthContextHolder.getUserId(request);
        //获取userTempId
        String userTempId = AuthContextHolder.getUserTempId(request);
        //获取列表数据
       List<CartInfo>  cartInfoList=cartInfoService.getCartList(userId,userTempId);


        return Result.ok(cartInfoList);
    }


    /**
     * /api/cart/addToCart/{skuId}/{skuNum}
     * 加入购物车
     * @param skuId
     * @param skuNum
     * @return
     */
    @GetMapping("/addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){
        //获取用户id
        String userId = AuthContextHolder.getUserId(request);
        //判断
        if(StringUtils.isEmpty(userId)){
            userId=AuthContextHolder.getUserTempId(request);
        }

        //加入购物车
        cartInfoService.addToCart(userId,skuId,skuNum);

        return Result.ok();

    }
}
