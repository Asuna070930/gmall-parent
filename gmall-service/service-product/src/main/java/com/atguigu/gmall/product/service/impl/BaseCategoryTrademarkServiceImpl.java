package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.atguigu.gmall.product.mapper.BaseCategoryTrademarkMapper;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseCategoryTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class BaseCategoryTrademarkServiceImpl implements BaseCategoryTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;
    @Autowired
    private BaseCategoryTrademarkMapper baseCategoryTrademarkMapper;
    /**
     * 根据category3Id获取品牌列表
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findTrademarkList(Long category3Id) {

        //构建查询对象
        QueryWrapper<BaseCategoryTrademark> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        //根据三级分类id查询关联的品牌id集合
        List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTrademarkMapper.selectList(queryWrapper);
        //处理结果，返回品牌id列表 stream流
        //filter 如果对集合处理之前和处理之后，类型一样选择
        //map 如果对集合处理之前和处理之后，类型不一样选择  List<BaseCategoryTrademark>--->List<Long>
        if(!CollectionUtils.isEmpty(baseCategoryTrademarks)){

            List<Long> trademarkIds = baseCategoryTrademarks.stream().map(baseCategoryTrademark -> {

                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());

            //根据id集合查询品牌列表
            //第一种方式：filter过滤
            //查询所有的品牌
//            List<BaseTrademark> baseTrademarks = baseTrademarkMapper.selectList(null);
//            //过滤和当前关联的品牌
//            List<BaseTrademark> baseTrademarkList = baseTrademarks.stream().filter(baseTrademark -> {
//
//
//                return trademarkIds.contains(baseTrademark.getId());
//            }).collect(Collectors.toList());

            //----第一种结束

//            第二种开始 原始sql方式 id =1,2,3
            //sql语句：select *from base_trademark where  id in (1,2,3);
            QueryWrapper<BaseTrademark> trademarkQueryWrapper =new QueryWrapper<>();
            trademarkQueryWrapper.in("id",trademarkIds);
            List<BaseTrademark> baseTrademarkList = baseTrademarkMapper.selectList(trademarkQueryWrapper);
            return baseTrademarkList;


        }







        return new ArrayList<>();
    }

    /**
     * 删除分类品牌关联
     * @param category3Id
     * @param trademarkId
     */
    @Override
    public void remove(Long category3Id, Long trademarkId) {


        //构建查询器
        QueryWrapper<BaseCategoryTrademark> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        queryWrapper.eq("trademark_id",trademarkId);
        //执行取消关联（删除）
        baseCategoryTrademarkMapper.delete(queryWrapper);

    }

    /**
     * 根据category3Id获取可选品牌列表
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseTrademark> findCurrentTrademarkList(Long category3Id) {
        //查询当前份分类id关联的分类品牌集合
        QueryWrapper<BaseCategoryTrademark> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("category3_id",category3Id);
        List<BaseCategoryTrademark> baseCategoryTrademarks = baseCategoryTrademarkMapper.selectList(queryWrapper);


        //根据查询结果获取关联的品牌id
        if(!CollectionUtils.isEmpty(baseCategoryTrademarks)){

            List<Long> trademarkIds = baseCategoryTrademarks.stream().map(baseCategoryTrademark -> {

                return baseCategoryTrademark.getTrademarkId();
            }).collect(Collectors.toList());



            //查询所有品牌
            List<BaseTrademark> baseTrademarks = baseTrademarkMapper.selectList(null);
            //过滤已经关联的品牌
            List<BaseTrademark> trademarkList = baseTrademarks.stream().filter(baseTrademark -> {

                return !trademarkIds.contains(baseTrademark.getId());
            }).collect(Collectors.toList());

            return trademarkList;

        }

        return baseTrademarkMapper.selectList(null);
    }

    /**
     * 保存分类品牌关联
     * /admin/product/baseCategoryTrademark/save
     * @param categoryTrademarkVo
     */
    @Override
    public void save(CategoryTrademarkVo categoryTrademarkVo) {

        //获取关联品牌id列表
        List<Long> trademarkIdList = categoryTrademarkVo.getTrademarkIdList();
        Long category3Id = categoryTrademarkVo.getCategory3Id();
        //拼接关联对象
        if(!CollectionUtils.isEmpty(trademarkIdList)){

            trademarkIdList.stream().forEach(trademarkId->{

                //创建关联对象
                BaseCategoryTrademark baseCategoryTrademark=new BaseCategoryTrademark();
                baseCategoryTrademark.setCategory3Id(category3Id);
                baseCategoryTrademark.setTrademarkId(trademarkId);
                //保存

                baseCategoryTrademarkMapper.insert(baseCategoryTrademark);

            });

        }



    }
}
