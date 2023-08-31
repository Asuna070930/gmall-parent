package com.atguigu.gmall.weball.controller;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Controller
public class SeckillGoodsController {

    @Resource
    private ActivityFeignClient activityFeignClient;



    /**
     * 确认订单
     * @param model
     * @return
     */
    @GetMapping("/seckill/trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = activityFeignClient.trade();

        model.addAllAttributes(result.getData());

        return "seckill/trade";
    }

    /**
     * 跳转到抢购页面
     * @return
     */
    @GetMapping("/seckill/queue.html")
    public String queue(HttpServletRequest request){
        //获取skuId
        String skuId = request.getParameter("skuId");
        //获取skuIdStr
        String skuIdStr = request.getParameter("skuIdStr");
        //响应数据
        request.setAttribute("skuId",skuId);
        request.setAttribute("skuIdStr",skuIdStr);


        return "seckill/queue";
    }

    /**
     * 跳转到秒杀详情
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/seckill/{skuId}.html")
    public String item(@PathVariable String skuId,Model model){

        //查询
        Result<SeckillGoods> result = activityFeignClient.getSeckillGoods(skuId);
        //响应
        model.addAttribute("item",result.getData());


        return "seckill/item";

    }

    /**
     * 跳转到秒杀列表
     * @return
     */
    @GetMapping("/seckill.html")
    public String index(Model model){
        Result<List<SeckillGoods>> result = activityFeignClient.findAll();
        model.addAttribute("list",result.getData());
        return "seckill/index";
    }
}
