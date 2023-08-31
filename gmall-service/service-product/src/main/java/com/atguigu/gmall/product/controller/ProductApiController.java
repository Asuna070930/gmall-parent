package com.atguigu.gmall.product.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.atguigu.gmall.product.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductApiController {

    @Autowired
    private ManagerService managerService;

    @Autowired
    private BaseTrademarkService trademarkService;



    /**
     *根据品牌Id 获取品牌数据
     * /api/product/inner/getTrademark/{tmId}
     * @param tmId
     * @return
     */
    @GetMapping("/inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){


        return trademarkService.getById(tmId);
    }
    /**
     * /api/product/getBaseCategoryList
     * 获取首页分类数据
     * @return
     */
    @GetMapping("/getBaseCategoryList")
    public Result<List<JSONObject>> getBaseCategoryList(){

        //JSONObject
        List<JSONObject> mapList=managerService.getBaseCategoryList();

        return Result.ok(mapList);
    }


    /**
     * //api/product/inner/getAttrList/{skuId}
     * 根据skuId 获取平台属性数据
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){


        return managerService.getAttrList(skuId);
    }


    /**
     * /api/product/inner/getSkuValueIdsMap/{spuId}
     * 商品切换关系查询
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSkuValueIdsMap/{spuId}")
    public Map<String,String> getSkuValueIdsMap(@PathVariable Long spuId){

        return managerService.getSkuValueIdMap(spuId);
    }


    /**
     * /api/product/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}
     * 根据spuId,skuId 获取销售属性数据
     * @param skuId
     * @param spuId
     * @return
     */
    @GetMapping("/inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){


        return managerService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }



    /**
     * //api/product/inner/getCategoryView/{category3Id}
     * 根据三级分类id获取分类信息
     * @param category3Id
     * @return
     */
    @GetMapping("/inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){

        return managerService.getCategoryView(category3Id);
    }


    /**
     * 根据spuId 获取海报数据
     * /api/product/inner/findSpuPosterBySpuId/{spuId}
     * @param spuId
     * @return
     */
    @GetMapping("/inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){

        return managerService.findSpuPosterBySpuId(spuId);
    }


    /**
     * /api/product/inner/getSkuPrice/{skuId}
     * 实时价格查询
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){

        return managerService.getSkuPrice(skuId);
    }

    /**
     * /api/product/inner/getSkuInfo/{skuId}
     * 根据skuId获取SkuInfo
     * @param skuId
     * @return
     */
    @GetMapping("/inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){


        return managerService.getSkuInfo(skuId);
    }

}
