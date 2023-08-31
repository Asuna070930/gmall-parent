package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/product/baseTrademark")
public class BaseTrademarkController {


    @Autowired
    private BaseTrademarkService baseTrademarkService;




    /**
     * 保存品牌
     * admin/product/baseTrademark/save
     * @param baseTrademark
     * @return
     */
    @PostMapping("/save")
    public Result saveTradeMark(@RequestBody BaseTrademark baseTrademark){
        baseTrademarkService.save(baseTrademark);

        return Result.ok();

    }


    /**
     * 修改品牌
     * admin/product/baseTrademark/update
     * @param baseTrademark
     * @return
     */
    @PutMapping("/update")
    public Result updateTrademark(@RequestBody BaseTrademark baseTrademark){

        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    /**
     * 根据id删除品牌信息
     *     ///admin/product/baseTrademark/remove/{id}
     * @param id
     * @return
     */
    @DeleteMapping("/remove/{id}")
    public Result deleteTrademark(@PathVariable Long id){

        baseTrademarkService.removeById(id);
        return Result.ok();
    }



    /**
     *
     * 根据id获取品牌对象
     *
     * /admin/product/baseTrademark/get/{id}
     * @param id
     * @return
     */
    @GetMapping("/get/{id}")
    public Result getBaseTrademark(@PathVariable Long id ){


        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);


    }



    /**
     * 品牌查询分页列表
     * /admin/product/baseTrademark/{page}/{limit}
     *
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/{page}/{limit}")
    public Result baseTrademarkPage(@PathVariable Long page,
                                    @PathVariable Long limit){

        //封装分页查询对象
        Page<BaseTrademark> trademarkPage=new Page<BaseTrademark>(page,limit);
        //查询
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.page(trademarkPage, new QueryWrapper<BaseTrademark>().orderByAsc("id"));

        return Result.ok(baseTrademarkIPage);

    }

}
