package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product")
public class SpuManagerController {

    @Autowired
    private ManagerService managerService;


    /**
     *  /admin/product/saveSpuInfo
     * 保存spu
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        managerService.saveSpuInfo(spuInfo);

        return Result.ok();
    }


    /**
     * admin/product/baseSaleAttrList
     * 获取销售属性数据
     * @return
     */
    @GetMapping("/baseSaleAttrList")
    public Result baseSaleAttrList(){

      List<BaseSaleAttr> baseSaleAttrs=  managerService.baseSaleAttrList();
        return Result.ok(baseSaleAttrs);
    }
    /**
     * /admin/product/{page}/{limit}
     * spu分类列表
     * @param page
     * @param limit
     * @param spuInfo
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result selectSpuByCategory3IdPage(@PathVariable Long page,
                                             @PathVariable Long limit,
                                             SpuInfo spuInfo){

        //分装分类对象
        Page<SpuInfo> spuInfoPage=new Page<>(page,limit);

        IPage<SpuInfo> spuInfoIPage=managerService.selectSpuByCategory3IdPage(spuInfoPage,spuInfo);


        return Result.ok(spuInfoIPage);

    }
}
