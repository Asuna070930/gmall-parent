package com.atguigu.gmall.product.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.cache.GmallCache;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.mapper.*;
import com.atguigu.gmall.product.service.ManagerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("all")
public class ManagerServiceImpl implements ManagerService {

    @Autowired
    private BaseCategory1Mapper baseCategory1Mapper;

    @Autowired
    private BaseCategory2Mapper baseCategory2Mapper;
    @Autowired
    private BaseCategory3Mapper baseCategory3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuPosterMapper spuPosterMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private BaseCategoryViewMapper baseCategoryViewMapper;
    
    @Autowired
    private RabbitService rabbitService;

    /**
     * 获取一级分类列表
     *
     * @return
     */
    @Override
    public List<BaseCategory1> getCategory1() {

        //条件构造器
        QueryWrapper<BaseCategory1> queryWrapper = new QueryWrapper<>();
//        LambdaQueryWrapper<BaseCategory1> queryWrapper=new LambdaQueryWrapper<>();
//        queryWrapper.orderByAsc(BaseCategory1::getId);
        queryWrapper.orderByAsc("id");
        List<BaseCategory1> category1List = baseCategory1Mapper.selectList(queryWrapper);

        return category1List;
    }

    /**
     * 获取二级分类数据
     *
     * @param category1Id
     * @return
     */
    @Override
    public List<BaseCategory2> getCategory2(Long category1Id) {
        //SELECT *FROM base_category2 WHERE category1_id =category1Id
        //条件构造器
        LambdaQueryWrapper<BaseCategory2> queryWrapper = new LambdaQueryWrapper<>();
        //设置条件
        queryWrapper.eq(BaseCategory2::getCategory1Id, category1Id);

        //执行查询
        return baseCategory2Mapper.selectList(queryWrapper);
    }

    /**
     * 获取三级分类数据
     *
     * @param category2Id
     * @return
     */
    @Override
    public List<BaseCategory3> getCategory3(Long category2Id) {
        //SELECT *FROM base_category3 WHERE category2_id =category2Id
        //条件构造器
        LambdaQueryWrapper<BaseCategory3> queryWrapper = new LambdaQueryWrapper<>();
        //设置条件
        queryWrapper.eq(BaseCategory3::getCategory2Id, category2Id);

        //执行查询
        return baseCategory3Mapper.selectList(queryWrapper);
    }

    /**
     * 根据分类Id 获取平台属性集合
     *
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    @Override
    public List<BaseAttrInfo> attrInfoList(Long category1Id, Long category2Id, Long category3Id) {

        return baseAttrInfoMapper.selectAttrInfoList(category1Id, category2Id, category3Id);
    }

    /**
     * 保存-修改属性
     *
     * @param baseAttrInfo 问题：操作两张表？
     * @Transactional:默认情况下只能回滚RuntimeException SqlException
     * IOException
     * 不能实现回滚
     * @Transactional(rollbackFor = Exception.class)
     * <p>
     * 新增和修改的区别：
     * <p>
     * 新增没有id
     * 修改：修改之前都有回显，再次提交存在id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {


        Long attrId = baseAttrInfo.getId();
        if (attrId != null) {

            //修改
            baseAttrInfoMapper.updateById(baseAttrInfo);
            //删除之前关联的平台属性值
            //构建条件构造器
            QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
            //delete from base_attr_value where attr_id =111
            queryWrapper.eq("attr_id", attrId);
            baseAttrValueMapper.delete(queryWrapper);


        } else {
            //保存平台属性
            baseAttrInfoMapper.insert(baseAttrInfo);

        }

        //保存平台属性值
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        //判断
        if (!CollectionUtils.isEmpty(attrValueList)) {

            for (BaseAttrValue baseAttrValue : attrValueList) {

                //补充数据
                baseAttrValue.setAttrId(baseAttrInfo.getId());

                baseAttrValueMapper.insert(baseAttrValue);
            }
        }


    }

    /**
     * 根据平台属性Id 获取到平台属性值集合
     *
     * @param attrId
     * @return
     */
    @Override
    public List<BaseAttrValue> getAttrValueList(Long attrId) {

        //select *from base_attr_value where attr_id=attrId
        //构建查询器
        QueryWrapper<BaseAttrValue> queryWrapper = new QueryWrapper<>();
        //添加条件
        queryWrapper.eq("attr_id", attrId);
        //添加排序
        queryWrapper.orderByAsc("id");

        List<BaseAttrValue> attrValueList = baseAttrValueMapper.selectList(queryWrapper);

        return attrValueList;
    }

    /**
     * 根据id查询平台属性
     *
     * @param attrId
     * @return
     */
    @Override
    public BaseAttrInfo getAttrInfoById(Long attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectById(attrId);
        //封装属性值集合
        List<BaseAttrValue> attrValueList = getAttrValueList(attrId);
        //判断
        if (baseAttrInfo != null) {
            baseAttrInfo.setAttrValueList(attrValueList);
        }
        return baseAttrInfo;
    }

    /**
     * spu分页列表
     *
     * @param spuInfoPage
     * @param spuInfo
     * @return
     */
    @Override
    public IPage<SpuInfo> selectSpuByCategory3IdPage(Page<SpuInfo> spuInfoPage, SpuInfo spuInfo) {

        //构建查询条件
        QueryWrapper<SpuInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("category3_id", spuInfo.getCategory3Id());
        queryWrapper.orderByAsc("id");

        return spuInfoMapper.selectPage(spuInfoPage, queryWrapper);
    }

    /**
     * 获取销售属性数据
     *
     * @return
     */
    @Override
    public List<BaseSaleAttr> baseSaleAttrList() {
        return baseSaleAttrMapper.selectList(null);
    }

    /**
     * 保存spu
     *
     * @param spuInfo 涉及到的表
     *                1.spu_info
     *                2.spu_sale_attr
     *                3.spu_sale_attr_value
     *                4.spu_image
     *                5.spu_poster
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        //保存spuInfo
        spuInfoMapper.insert(spuInfo);
        //保存spu_sale_attr
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if (!CollectionUtils.isEmpty(spuSaleAttrList)) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {

                spuSaleAttr.setSpuId(spuInfo.getId());
                //保存
                spuSaleAttrMapper.insert(spuSaleAttr);

                //获取属性值集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                //保存 spu_sale_attr_value
                if (!CollectionUtils.isEmpty(spuSaleAttrValueList)) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrName(spuSaleAttr.getSaleAttrName());

                        spuSaleAttrValueMapper.insert(spuSaleAttrValue);

                    }

                }


            }


        }

        //保存spu_image
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if (!CollectionUtils.isEmpty(spuImageList)) {
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insert(spuImage);
            }
        }

        //保存spu_poster
        List<SpuPoster> spuPosterList = spuInfo.getSpuPosterList();
        if (!CollectionUtils.isEmpty(spuPosterList)) {
            for (SpuPoster spuPoster : spuPosterList) {

                spuPoster.setSpuId(spuInfo.getId());
                spuPosterMapper.insert(spuPoster);
            }
        }

    }

    /**
     * 根据spuId 获取spuImage 集合
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuImage> spuImageList(Long spuId) {

        //构建查询条件
        QueryWrapper<SpuImage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);
        List<SpuImage> spuImageList = spuImageMapper.selectList(queryWrapper);

        return spuImageList;
    }

    /**
     * 根据spuId 查询销售属性
     *
     * @param spuId
     * @return
     */
    @Override
    public List<SpuSaleAttr> spuSaleAttrList(Long spuId) {

        return spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
    }

    /**
     * 保存sku
     *
     * @param skuInfo 涉及到的表：
     *                sku_info
     *                sku_image
     *                sku_sale_attr_value
     *                sku_attr_value
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkuInfo(SkuInfo skuInfo) {
        //保存skuinfo
        skuInfoMapper.insert(skuInfo);
        //保存图片
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        //判断
        if (!CollectionUtils.isEmpty(skuImageList)) {
            skuImageList.forEach(skuImage -> {

                skuImage.setSkuId(skuInfo.getId());

                skuImageMapper.insert(skuImage);

            });
        }

        // sku_sale_attr_value
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        //判断
        if (!CollectionUtils.isEmpty(skuSaleAttrValueList)) {
            skuSaleAttrValueList.forEach(skuSaleAttrValue -> {
                skuSaleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValue.setSpuId(skuInfo.getSpuId());

                skuSaleAttrValueMapper.insert(skuSaleAttrValue);
            });
        }

        //sku_attr_value
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        //判断
        if (!CollectionUtils.isEmpty(skuAttrValueList)) {

            skuAttrValueList.forEach(skuAttrValue -> {

                skuAttrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insert(skuAttrValue);

            });
        }


//        //条件到布隆过滤器
//        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
//        //添加
//        bloomFilter.add(skuInfo.getId());


    }

    /**
     * 分页sku列表
     *
     * @param skuInfoPage
     * @return
     */
    @Override
    public IPage<SkuInfo> findSkuListBypage(Page<SkuInfo> skuInfoPage) {

        return skuInfoMapper.selectPage(skuInfoPage, new QueryWrapper<SkuInfo>().orderByDesc("id"));
    }

    /**
     * 商品下架
     *
     * @param skuId
     */
    @Override
    public void cancelSale(Long skuId) {

        //sql= update sku_info set is_sale=0 where id =21
        //封装数据
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(0);
        skuInfoMapper.updateById(skuInfo);
        //发送消息
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_GOODS,
                MqConst.ROUTING_GOODS_LOWER,skuId);
    }

    /**
     * 商品上架
     *
     * @param skuId
     */
    @Override
    public void onSale(Long skuId) {
        //sql= update sku_info set is_sale=1 where id =21
        //封装数据
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        skuInfo.setIsSale(1);
        skuInfoMapper.updateById(skuInfo);
        //发送消息
        rabbitService.sendMessage(
                MqConst.EXCHANGE_DIRECT_GOODS,
                MqConst.ROUTING_GOODS_UPPER,skuId);

    }

    /**
     * 根据skuId获取SkuInfo
     *
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = "sku:", suffix = ":info")
    public SkuInfo getSkuInfo(Long skuId) {


//         return getSkuInfoRedisson(skuId);
//        return getSkuInfoRedis(skuId);

        return getSkuInfoDB(skuId);
    }


    public static void main(String[] args) {

        String[] str = new String[]{"21"};
        System.out.println(Arrays.toString(str));
        //sku:[Ljava.lang.String;@17f6480:info


    }

    @Autowired
    private RedissonClient redissonClient;

    /**
     * redisson实现数据缓存和分布式锁处理
     *
     * @param skuId
     * @return 1.定义key，从redis中尝试获取数据
     * * 2.有，直接返回
     * * 3.没有数据
     * * 4.获取分布式锁
     * * 没有 自旋
     * * 获取锁
     * * 5.查询数据数据库
     * * 有：存储redis，返回数据
     * * 没有：存储null,返回数据
     * * <p>
     * * 6.查询数据库
     */
    private SkuInfo getSkuInfoRedisson(Long skuId) {
        try {
            //定义redis获取数据的key sku:21:info   skuInfo
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //从redis中获取数据
            SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断
            if (skuInfo == null) {
                //定义锁key
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //获取锁
                RLock lock = redissonClient.getLock(lockKey);
                //加锁
                boolean flag = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX1, RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);
                //判断
                if (flag) {
                    try {
                        //二次检查，屏蔽通过第一判断，中间有线程已经存储数据到redis中，避免再次查询mysql
                        skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
                        //判断
                        if (skuInfo != null) {

                            return skuInfo;
                        }


                        //查询数据库
                        //获取到了锁，查询数据库
                        skuInfo = getSkuInfoDB(skuId);
                        //判断
                        if (skuInfo == null) {
                            skuInfo = new SkuInfo();
                            //数据库没有数据
                            redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);
                        } else {
                            //数据库有数据
                            redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TIMEOUT, TimeUnit.SECONDS);
                        }
                        return skuInfo;
                    } finally {
                        //释放锁
                        lock.unlock();
                    }
                } else {
                    Thread.sleep(100);
                    return getSkuInfoRedisson(skuId);
                }


            } else {
                //有返回数据
                return skuInfo;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        //兜底处理
        return getSkuInfoDB(skuId);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * redis实现增强获取skuInfo
     * 作用：数据缓存+redis实现分布式锁
     *
     * @param skuId
     * @return 1.定义key，从redis中尝试获取数据
     * 2.有，直接返回
     * 3.没有数据
     * 4.获取分布式锁
     * 没有 自旋
     * 获取锁
     * 5.查询数据数据库
     * 有：存储redis，返回数据
     * 没有：存储null,返回数据
     * <p>
     * 6.查询数据库
     */
    private SkuInfo getSkuInfoRedis(Long skuId) {

        try {
            //定义redis获取数据的key sku:21:info   skuInfo
            String skuKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKUKEY_SUFFIX;
            //从redis中获取数据
            SkuInfo skuInfo = (SkuInfo) redisTemplate.opsForValue().get(skuKey);
            //判断
            if (skuInfo == null) { //没有数据，查询数据库，之前要先获取锁
                //制定锁keylock  sku:21:lock
                String lockKey = RedisConst.SKUKEY_PREFIX + skuId + RedisConst.SKULOCK_SUFFIX;
                //制定锁值生成uuid
                String uuid = UUID.randomUUID().toString().replaceAll("-", "");
                //获取锁
                Boolean flag = redisTemplate.opsForValue().setIfAbsent(lockKey, uuid, 7, TimeUnit.SECONDS);
                //判断
                if (flag) {
                    try {
                        //获取到了锁，查询数据库
                        skuInfo = getSkuInfoDB(skuId);
                        //判断
                        if (skuInfo == null) {
                            skuInfo = new SkuInfo();
                            //数据库没有数据
                            redisTemplate.opsForValue().set(skuKey, skuInfo, RedisConst.SKUKEY_TEMPORARY_TIMEOUT, TimeUnit.SECONDS);

                        } else {

                            //数据库有数据
                            redisTemplate.opsForValue().set(skuKey, skuInfo, 10, TimeUnit.SECONDS);

                        }
                        return skuInfo;
                    } finally {

                        //释放锁
                        //使用lua脚本改造
                        String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                                "then\n" +
                                "    return redis.call(\"del\",KEYS[1])\n" +
                                "else\n" +
                                "    return 0\n" +
                                "end";
                        //创建脚本对象
                        DefaultRedisScript redisScript = new DefaultRedisScript();
                        //设置脚本
                        redisScript.setScriptText(script);
                        //设置返回值类型
                        redisScript.setResultType(Long.class);
                        //发送脚本
                        redisTemplate.execute(redisScript, Arrays.asList(lockKey), uuid);

                    }


                } else {

                    //没有获取到锁，自旋等待再次尝试
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    return getSkuInfoRedis(skuId);

                }


            } else {
                //缓存中有数据，直接返回即可
                return skuInfo;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }


        //操作redis出现异常，兜底方法
        return getSkuInfoDB(skuId);
    }


    /**
     * 从数据中查询
     *
     * @param skuId
     * @return
     */
    private SkuInfo getSkuInfoDB(Long skuId) {
        //查询skuInfo根据id
        SkuInfo skuInfo = skuInfoMapper.selectById(skuId);
        //查询图片列表
        LambdaQueryWrapper<SkuImage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SkuImage::getSkuId, skuId);
        List<SkuImage> skuImages = skuImageMapper.selectList(queryWrapper);
        //封装数据
        if (skuInfo != null) {

            skuInfo.setSkuImageList(skuImages);
        }


        return skuInfo;
    }

    /**
     * 实时价格查询
     *
     * @param skuId
     * @return
     */
    @Override
    public BigDecimal getSkuPrice(Long skuId) {
        //获取锁
        RLock lock = redissonClient.getLock(skuId + RedisConst.SKULOCK_SUFFIX);
        //加锁
        lock.lock(RedisConst.SKULOCK_EXPIRE_PX2, TimeUnit.SECONDS);

        try {
            SkuInfo skuInfo = skuInfoMapper.selectById(skuId);

            //判断
            if (skuInfo != null) {

                return skuInfo.getPrice();
            }
        } finally {

            lock.unlock();
        }

        return new BigDecimal("0");
    }

    /**
     * 根据spuId 获取海报数据
     *
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "findSpuPosterBySpuId:", suffix = ":info")
    @Transactional(rollbackFor = Exception.class)
    public List<SpuPoster> findSpuPosterBySpuId(Long spuId) {

        //封装查询条件
        QueryWrapper<SpuPoster> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spu_id", spuId);


        return spuPosterMapper.selectList(queryWrapper);
    }

    /**
     * 根据三级分类id获取分类信息
     *
     * @param category3Id
     * @return
     */
    @Override
    @GmallCache(prefix = "categoryView:", suffix = ":info")
    public BaseCategoryView getCategoryView(Long category3Id) {
        return baseCategoryViewMapper.selectById(category3Id);
    }

    /**
     * 根据spuId,skuId 获取销售属性数据
     *
     * @param skuId
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "spuSaleAttrListCheckBySku:", suffix = ":info")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId, Long spuId) {


        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuId, spuId);
    }

    /**
     * 商品切换关系查询
     *
     * @param spuId
     * @return
     */
    @Override
    @GmallCache(prefix = "skuValueIdMap:", suffix = ":info")
    public Map<String, String> getSkuValueIdMap(Long spuId) {
        //创建map
        Map<String, String> resultMap = new HashMap<>();
        //查询切换关系
        List<Map> mapList = skuSaleAttrValueMapper.selectSkuValueIdMap(spuId);
        //遍历处理
        mapList.stream().forEach(map -> {
            resultMap.put(String.valueOf(map.get("value_ids")), String.valueOf(map.get("sku_id")));
//            Object value_dis = map.get("value_ids");
//            Object sku_id = map.get("sku_id");
//            resultMap.put(value_dis.toString(),sku_id.toString());
//            resultMap.put(value_dis.toString(),sku_id.toString());
//            resultMap.put(value_dis.toString(),sku_id.toString());
//            resultMap.put(value_dis.toString(),sku_id.toString());
        });


        return resultMap;
    }

    /**
     * 根据skuId 获取平台属性数据
     *
     * @param skuId
     * @return
     */
    @Override
    @GmallCache(prefix = "attrList:", suffix = ":info")
    public List<BaseAttrInfo> getAttrList(Long skuId) {
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrList(skuId);
        return baseAttrInfoList;
    }

    /**
     * 获取首页分类数据
     *
     * @return 1.8
     * 集合操作的便利方式：
     * Stream
     * map:
     * filter:
     * <p>
     * Collectors.groupingBy:
     * 分组：根据集合中对象的属性值，将集合按照map结构进行分组
     * <p>
     * key: 分组根据的id值
     * value:
     * 这个集合中指定分组根据id值所对应的所有记录
     */
    @Override
    @GmallCache(prefix = "baseCategoryList:",suffix = ":info")
    public List<JSONObject> getBaseCategoryList() {

        //查询所有分类
        List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
        //分组处理一级分类
        Map<Long, List<BaseCategoryView>> category1Map = baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
        //定义序号
        int index=1;
        List<JSONObject> objects=new ArrayList<>();
        //遍历一级分类后的数据
        for (Map.Entry<Long, List<BaseCategoryView>> category1Entry : category1Map.entrySet()) {

           //创建封装一级分类对象
            JSONObject obj1=new JSONObject();
            //获取一级分类的id
            Long category1Id = category1Entry.getKey();
            obj1.put("index",index);
            obj1.put("categoryId",category1Id);
            //获取一级份类名称
            List<BaseCategoryView> category2List = category1Entry.getValue();
            String category1Name = category2List.get(0).getCategory1Name();
            obj1.put("categoryName",category1Name);

            List<JSONObject> objects2=new ArrayList<>();
            //封装二级分类数据
            Map<Long, List<BaseCategoryView>> category2Map = category2List.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
            //遍历处理二级分类结构
            for (Map.Entry<Long, List<BaseCategoryView>> category2Entry : category2Map.entrySet()) {
                //创建对象封装二级分类数据
                JSONObject obj2=new JSONObject();
                //获取二级分类id
                Long category2Id = category2Entry.getKey();
                obj2.put("categoryId",category2Id);
                //获取二级分类name
                List<BaseCategoryView> category3List = category2Entry.getValue();
                String category2Name = category3List.get(0).getCategory2Name();
                obj2.put("categoryName",category2Name);

                //创建三级分类集合
                List<JSONObject> objects3=new ArrayList<>();
                //封装三级分类数据
                for (BaseCategoryView baseCategoryView : category3List) {
                    JSONObject obj3=new JSONObject();
                    obj3.put("categoryId",baseCategoryView.getCategory3Id());
                    obj3.put("categoryName",baseCategoryView.getCategory3Name());
                    objects3.add(obj3);
                }


                //设置三级分类数据
                obj2.put("categoryChild",objects3);
                objects2.add(obj2);

            }



            //设置二级分类数据
            obj1.put("categoryChild",objects2);

            objects.add(obj1);

        }


        return objects;
    }

}
