package com.atguigu.gmall.weball.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;

@Controller
public class IndexController {

    @Resource
    private ProductFeignClient productFeignClient;

    /**
     * 普通渲染实现
     * @param model
     * @return
     */
    @GetMapping({"/","/index.html","/index"})
    public String index(Model model){

        //获取首页需要的分类数据
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",result.getData());
        return "index/index";
    }

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 生成页面，nginx部署
     * @return
     */
    @GetMapping("/createIndex")
    @ResponseBody
    public Result createIndex(){


        //创建上下文对象
        Context context=new Context();
        Result<List<JSONObject>> result = productFeignClient.getBaseCategoryList();
        context.setVariable("list",result.getData());
        //
        Writer writer= null;
        try {
            writer = new FileWriter("D:\\gmall-index\\index.html");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //生成页面
        templateEngine.process("index/index",context,writer);

        return Result.ok();
    }


}
