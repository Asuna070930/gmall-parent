package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/product/baseCategoryTrademark")
public class BaseCategoryTrademarkController {

    @Autowired
    private BaseCategoryTrademarkService baseCategoryTrademarkService;




    /**
     * 保存分类品牌关联
     * /admin/product/baseCategoryTrademark/save
     * @param categoryTrademarkVo
     * @return
     */
    @PostMapping("/save")
    public Result save(@RequestBody CategoryTrademarkVo categoryTrademarkVo){


        baseCategoryTrademarkService.save(categoryTrademarkVo);

        return Result.ok();
    }

    /**
     * 根据category3Id获取可选品牌列表
     *  /admin/product/baseCategoryTrademark/findCurrentTrademarkList/{category3Id}
     * @param category3Id
     * @return
     */
    @GetMapping("/findCurrentTrademarkList/{category3Id}")
    public Result findCurrentTrademarkList(@PathVariable Long category3Id){

        List<BaseTrademark> baseTrademarks=baseCategoryTrademarkService.findCurrentTrademarkList(category3Id);
        return Result.ok(baseTrademarks);
    }

    /**
     * 删除分类品牌关联
     * /admin/product/baseCategoryTrademark/remove/{category3Id}/{trademarkId}
     * @param category3Id
     * @param trademarkId
     * @return
     */
    @DeleteMapping("/remove/{category3Id}/{trademarkId}")
    public Result remove(@PathVariable Long category3Id,
                         @PathVariable Long trademarkId){
        baseCategoryTrademarkService.remove(category3Id,trademarkId);

        return Result.ok();
    }

    /**
     * admin/product/baseCategoryTrademark/findTrademarkList/{category3Id}
     * 根据category3Id获取品牌列表
     * @param category3Id
     * @return
     */
    @GetMapping("/findTrademarkList/{category3Id}")
    public Result findTrademarkList(@PathVariable Long category3Id){

        List<BaseTrademark> baseTrademarks=baseCategoryTrademarkService.findTrademarkList(category3Id);

        return Result.ok(baseTrademarks);

    }

}
