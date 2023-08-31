package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.BaseAttrInfoMapper;
import com.atguigu.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/admin/product")
//@CrossOrigin
public class BaseManagerController {
    @Autowired
    private ManagerService managerService;




    /**
     * /admin/product/getAttrValueList/{attrId}
     * 根据平台属性Id 获取到平台属性值集合
     * @param attrId
     * @return
     */
    @GetMapping("/getAttrValueList/{attrId}")
    public Result getAttrValueList(@PathVariable Long attrId){

        //获取平台属性对象
       BaseAttrInfo baseAttrInfo= managerService.getAttrInfoById(attrId);
       //获取平台属性对象关联的属性值集合
        if(baseAttrInfo!=null){

            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            return Result.ok(attrValueList);
        }

         return Result.fail("没有对应的属性数据");


    }



    /**
     * 保存-修改属性
     * admin/product/saveAttrInfo
     * @param baseAttrInfo
     * @return
     */
    @PostMapping("/saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){

        managerService.saveAttrInfo(baseAttrInfo);


        return Result.ok();
    }



    /**
     *根据分类Id 获取平台属性集合
     * admin/product/attrInfoList/{category1Id}/{category2Id}/{category3Id}
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @GetMapping("/attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result attrInfoList(
            @PathVariable Long category1Id,
            @PathVariable Long category2Id,
            @PathVariable Long category3Id){
        List<BaseAttrInfo> baseAttrInfoList=managerService.attrInfoList(category1Id,category2Id,category3Id);



        return Result.ok(baseAttrInfoList);
    }


    /**
     * /admin/product/getCategory3/{category2Id}
     * 获取三级分类数据
     * @param category2Id
     * @return
     */
    @GetMapping("/getCategory3/{category2Id}")
    public Result getCategory3(@PathVariable Long category2Id ){

        List<BaseCategory3> category3List= managerService.getCategory3(category2Id);

        return Result.ok(category3List);

    }

    /**
     * /admin/product/getCategory2/{category1Id}
     * 获取二级分类数据
     * @param category1Id
     * @return
     */
    @GetMapping("/getCategory2/{category1Id}")
    public Result getCategory2(@PathVariable Long category1Id ){

       List<BaseCategory2> category2List= managerService.getCategory2(category1Id);

        return Result.ok(category2List);

    }



    /**
     * 获取一级分类列表
     * @return
     */
    @GetMapping("/getCategory1")
    public Result getCategory1(){

      List<BaseCategory1>  category1List=managerService.getCategory1();

        return Result.ok(category1List);
    }



}
