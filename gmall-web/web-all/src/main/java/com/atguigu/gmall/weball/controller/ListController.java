package com.atguigu.gmall.weball.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import javax.annotation.Resource;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {

    @Resource
    private ListFeignClient listFeignClient;

    /**
     * 搜索
     *
     * @return
     */
    @GetMapping("/list.html")
    public String search(SearchParam searchParam, Model model) {

        //调用搜索接口
        Result<Map<String, Object>> result = listFeignClient.search(searchParam);

        //拼接urlParam路径
        String urlParam = this.makeUrlParam(searchParam);
        model.addAttribute("urlParam", urlParam);

        String trademarkParam=this.makeTrademarkParam(searchParam.getTrademark());
        //品牌面包屑  品牌：荣耀
        model.addAttribute("trademarkParam",trademarkParam);
        //平台属性面包屑 机身内存:128G
        List<Map> propsParamList=this.makePropsParamList(searchParam.getProps());


        model.addAttribute("propsParamList",propsParamList);

        Map<String,Object> orderMap=this.makeOrderMap(searchParam.getOrder());
        //赋值默认排序方式，编排选择后的排序方式
        model.addAttribute("orderMap",orderMap);



        //响应数据
        model.addAllAttributes(result.getData());

        return "list/index";
    }

    /**
     *
     * order=1:asc
     * 赋值默认排序方式，编排选择后的排序方式
     * @param order
     * @return
     */
    private Map<String, Object> makeOrderMap(String order) {

        //创建封装对象
        Map<String,Object> orderMap=new HashMap<>();
        //判断
        if(!StringUtils.isEmpty(order)){
            //order=1:asc
            String[] split = order.split(":");
            //判断
            if(split!=null &&split.length==2){
                orderMap.put("type",split[0]);
                orderMap.put("sort",split[1]);
            }
        }else{
            //如果首次，默认根据热度降序排列
            orderMap.put("type",1);
            orderMap.put("sort","desc");

        }


        return orderMap;
    }

    /**
     * 平台属性面包屑
     * @param props
     * @return
     */
    private List<Map> makePropsParamList(String[] props) {

        //创建封装对象
        List<Map> mapList=new ArrayList<>();
        //判断
        if(props!=null &&props.length>0){
            for (String prop : props) {
                //props=23:6G:运行内存
                String[] split = prop.split(":");
                //判断
                if(split!=null &&split.length==3){

                    Map map=new HashMap();
                    map.put("attrId",split[0]);
                    map.put("attrName",split[2]);
                    map.put("attrValue",split[1]);
                    mapList.add(map);
                }
            }


        }

        return mapList;
    }


    /**
     * 凭借品牌面包屑
     * 品牌：荣耀
     * @param trademark
     * @return
     *
     * trademark=1:小米
     */
    private String makeTrademarkParam(String trademark) {
        //判断
        if(!StringUtils.isEmpty(trademark)){
            String[] split = trademark.split(":");
            //判断
            if(split!=null &&split.length==2){

                return "品牌:"+split[1];
            }

        }


        return "";
    }

    /**
     *
     * 一级参数 ？
     * http://list.gmall.com/list.html?category3Id=61
     * http://list.gmall.com/list.html?keyword=%E6%89%8B%E6%9C%BA
     * 拼接urlParam路径
     * 二级参数 &
     *
     * &trademark=1:小米
     * &props=23:6G:运行内存&props=24:128G:机身内存
     *
     *
     *
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        //字符串拼接对象
        StringBuilder builder = new StringBuilder();
        //判断关键字 一级参数
        if(!StringUtils.isEmpty(searchParam.getKeyword())){

            builder.append("keyword").append("="+searchParam.getKeyword());

        }
        //判断分类
        if(searchParam.getCategory1Id()!=null &&searchParam.getCategory1Id()!=0){


            builder.append("category1Id").append("="+searchParam.getCategory1Id());
        }
        if(searchParam.getCategory2Id()!=null &&searchParam.getCategory2Id()!=0){


            builder.append("category2Id").append("="+searchParam.getCategory2Id());
        }
        if(searchParam.getCategory3Id()!=null &&searchParam.getCategory3Id()!=0){


            builder.append("category3Id").append("="+searchParam.getCategory3Id());
        }

        //二级参数
        //品牌
        if(!StringUtils.isEmpty(searchParam.getTrademark())){
            //拼接的前提已经有了一级参数
            if(builder.length()>0){

                builder.append("&trademark=").append(searchParam.getTrademark());

            }




        }

        //平台属性
        String[] props = searchParam.getProps();
        //判断
        if(props!=null &&props.length>0){

            if(builder.length()>0){
                for (String prop : props) {

                    builder.append("&props=").append(prop);

                }


            }



        }


        return "list.html?"+builder.toString();
    }
}
