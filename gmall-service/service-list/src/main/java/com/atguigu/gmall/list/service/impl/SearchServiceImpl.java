package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.BaseAttrInfo;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Resource
    private ProductFeignClient productFeignClient;

    @Resource
    private RedisTemplate redisTemplate;
    /**
     * 商品的上架
     * @param skuId
     */
    @Override
    public void upperGoods(Long skuId) {
        //创建商品对象
        Goods goods=new Goods();

        //获取skuInfo

        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        if(skuInfo!=null){

            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setTitle(skuInfo.getSkuName());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setCreateTime(new Date());
            //品牌查询
            BaseTrademark trademark = productFeignClient.getTrademark(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
            //三级分类数据
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            goods.setCategory1Id(categoryView.getCategory1Id());
            goods.setCategory1Name(categoryView.getCategory1Name());
            goods.setCategory2Id(categoryView.getCategory2Id());
            goods.setCategory2Name(categoryView.getCategory2Name());
            goods.setCategory3Id(categoryView.getCategory3Id());
            goods.setCategory3Name(categoryView.getCategory3Name());

        }

        //查询平台属性数据
        List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);

        //判断
        if(!CollectionUtils.isEmpty(attrList)){
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {

                //创建SearchAttr
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                searchAttr.setAttrValue(baseAttrInfo.getAttrValueList().get(0).getValueName());

                return searchAttr;

            }).collect(Collectors.toList());

            goods.setAttrs(searchAttrList);

        }



        goodsRepository.save(goods);
    }

    /**
     * 商品下架
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {

        goodsRepository.deleteById(skuId);
    }

    /**
     * 更新商品的热度排名
     * @param skuId
     *
     * 问题：
     *     1.自增是非原子性
     *         redis-原子性
     *     需求：
     *       hotscore   21   8
     *                  22   6
     *                  23   5
     *    类型： zset        incr
     *
     *     2.修改太过频繁
     *         es-磁盘--实时数据更新
     *         每10次修改，记录redis
     *
     *
     */
    @Override
    public void incrHotScore(Long skuId) {

        //定义操作key
        String hotKey="hotscore";
        //统计自增
        Double count = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        //取模是10
        if(count%10==0){
            //获取
            Goods goods = goodsRepository.findById(skuId).get();
            //判断
            if(goods!=null){
                goods.setHotScore(Math.round(count));
                goodsRepository.save(goods);
            }
        }


    }

    @Resource
    private RestHighLevelClient client;

    /**
     * 商品搜索
     * @param searchParam
     * @return
     */
    @Override
    public SearchResponseVo search(SearchParam searchParam) {
        SearchResponseVo searchResponseVo= null;

        try {
            //创建请求对象，构建查询条件DSL
            SearchRequest searchRequest=this.builderDsl(searchParam);

            //发送请求到es
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            //处理结果
            searchResponseVo = this.parsedSearchResponseVo(searchResponse);

            //封装当前页
            searchResponseVo.setPageNo(searchParam.getPageNo());
            //封装每页条数
            searchResponseVo.setPageSize(searchParam.getPageSize());

            //计算总页数 =总条数/每天条数  8 /3= 2  3页   9  3  3

            if(searchResponseVo.getTotal()%searchParam.getPageSize()==0){
                //封装总页数
                searchResponseVo.setTotalPages(searchResponseVo.getTotal()/searchParam.getPageSize());

            }else{
                //封装总页数
                searchResponseVo.setTotalPages((searchResponseVo.getTotal()/searchParam.getPageSize())+1);
            }


            //计算总页数=(总条数+每页条数-1)/每页条数
            // (8+3-1)/3=3      (9+3-1)/3=3
//            long totalPages = (searchResponseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return searchResponseVo;
    }

    /**
     * 处理查询后的结果集转换为searchResponseVo
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parsedSearchResponseVo(SearchResponse searchResponse) {
        //创建响应对象
        SearchResponseVo searchResponseVo=new SearchResponseVo();

        //获取所有的聚合数据
        Map<String, Aggregation> mapAllAgg = searchResponse.getAggregations().asMap();

        if(!CollectionUtils.isEmpty(mapAllAgg)){

            //获取品牌聚合 对象
            ParsedLongTerms tmIdAgg = (ParsedLongTerms) mapAllAgg.get("tmIdAgg");
            if(tmIdAgg!=null){

                //List<SearchResponseTmVo>
                //获取品牌聚合所有数据
                List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
                    SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
                    //设置品牌id
                    long tmId = bucket.getKeyAsNumber().longValue();
                    searchResponseTmVo.setTmId(tmId);
                    //设置品牌名称
                    //获取品牌名称的聚合对象
                    ParsedStringTerms tmNameAgg = (ParsedStringTerms) bucket.getAggregations().asMap().get("tmNameAgg");
                    String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
                    searchResponseTmVo.setTmName(tmName);

                    //设置值品牌路径
                    ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) bucket.getAggregations().asMap().get("tmLogoUrlAgg");
                    String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
                    searchResponseTmVo.setTmLogoUrl(tmLogoUrl);


                    return searchResponseTmVo;
                }).collect(Collectors.toList());
                //设置品牌数据
                searchResponseVo.setTrademarkList(trademarkList);

            }

            //设置平台属性数据
            //List<SearchResponseAttrVo> attrsList
            ParsedNested attrsAgg = (ParsedNested) mapAllAgg.get("attrsAgg");

            //获取平台属性id的聚合对象
            ParsedLongTerms attrIdAgg = (ParsedLongTerms) attrsAgg.getAggregations().asMap().get("attrIdAgg");
           //封装平台集合
            List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                //设置平台属性id
                long attrId = bucket.getKeyAsNumber().longValue();
                searchResponseAttrVo.setAttrId(attrId);
                //平台属性名
                ParsedStringTerms attrNameAgg = (ParsedStringTerms) bucket.getAggregations().asMap().get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);
                //设置平台属性值集合

                ParsedStringTerms attrValueAgg = (ParsedStringTerms) bucket.getAggregations().asMap().get("attrValueAgg");

                List<String> attrValueList = attrValueAgg.getBuckets().stream().map(subBucket -> {


                    return subBucket.getKeyAsString();
                }).collect(Collectors.toList());
                //设置数据值集合
                searchResponseAttrVo.setAttrValueList(attrValueList);

                //返回封装平台对象
                return searchResponseAttrVo;
            }).collect(Collectors.toList());

            //设置到响应对象
            searchResponseVo.setAttrsList(attrsList);

        }


        //设置商品列表数据List<Goods> goodsList
        SearchHits hits = searchResponse.getHits();
         SearchHit[] searchHits = hits.getHits();
        //判断
        if(searchHits!=null &&searchHits.length>0){
            //获取封装的商品列表
            List<Goods> goodsList = Arrays.stream(searchHits).map(hit -> {

                //获取数据
                String sourceAsString = hit.getSourceAsString();
                Goods goods = JSONObject.parseObject(sourceAsString, Goods.class);
                //设置高亮数据
                if(hit.getHighlightFields()!=null&&hit.getHighlightFields().get("title")!=null){

                    String title = hit.getHighlightFields().get("title").getFragments()[0].toString();
                    //设置高亮字段
                    goods.setTitle(title);

                }


                return goods;
            }).collect(Collectors.toList());
            searchResponseVo.setGoodsList(goodsList);

        }





        //设置分页数据
        //获取总记录数据
        long totalSize = hits.getTotalHits().value;
        searchResponseVo.setTotal(totalSize);



        return searchResponseVo;
    }


    /**
     * 构建请求对象和DSL语句
     * @param searchParam
     * @return
     */
    public SearchRequest builderDsl(SearchParam searchParam){
        //创建请求对象
        SearchRequest searchRequest = new SearchRequest("goods");
        //条件构造器{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //多条条件构造
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断
        String keyword = searchParam.getKeyword();
        if(!StringUtils.isEmpty(keyword)){
            //多条件添加must
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));
        }


        if(searchParam.getCategory1Id()!=null&&searchParam.getCategory1Id()!=0){
            //设置一级分类过滤条件
            boolQueryBuilder.filter(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));


        }
        if(searchParam.getCategory2Id()!=null&&searchParam.getCategory2Id()!=0){
            //设置二级分类过滤条件
            boolQueryBuilder.filter(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));


        }
        if(searchParam.getCategory3Id()!=null&&searchParam.getCategory3Id()!=0){
            //设置三级分类过滤条件
            boolQueryBuilder.filter(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));


        }

        //判断品牌 &trademark=2:华为
        String trademark = searchParam.getTrademark();
        if(!StringUtils.isEmpty(trademark)){

            String[] split = trademark.split(":");
            //判断
            if(split!=null&&split.length==2){

                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }

        }

        //判断
        String[] props = searchParam.getProps();
        if(props!=null&&props.length>0){


            //遍历数组
            for (String prop : props) {
                //创建多条件对象
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

                //创建一个子多条件对象
                BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();

                String[] split = prop.split(":");
                //判断
                if(split!=null&&split.length==3){

                    //props=23:4G:运行内存&props=23:4G:运行内存
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                }

                //prop=23:4G:运行内存
                //nested类型连接
                boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));


                //设置到外层的条件构造器
                boolQueryBuilder.filter(boolQuery);
            }



        }



        //将多条件添加到条件构造器
        searchSourceBuilder.query(boolQueryBuilder);

        //排序 order=1:asc  order=1:desc order=2:asc  order=2:desc ===1:hotScore 2:price
        String order = searchParam.getOrder();
        if(!StringUtils.isEmpty(order)){
            //截取
            String[] split = order.split(":");
            //判断
            if(split!=null &split.length==2){

                //确定字段
                String filed="";
                switch (split[0]){
                    case "1":
                        filed="hotScore";
                        break;
                    case "2":
                        filed="price";
                        break;
                }

                searchSourceBuilder.sort(filed,"asc".equals(split[1])?SortOrder.ASC:SortOrder.DESC);

            }



        }else{
            //默认排序hotScore
            searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        }


        //创建高亮对象
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //添加字段
        highlightBuilder.field("title");
        //设置前缀
        highlightBuilder.preTags("<font color='red'>");
        //设置后缀
        highlightBuilder.postTags("</font>");
        //设置高亮
        searchSourceBuilder.highlighter(highlightBuilder);


        //分页
        //设置开始索引
        searchSourceBuilder.from((searchParam.getPageNo()-1)*searchParam.getPageSize());
        //设置每页条数
        searchSourceBuilder.size(searchParam.getPageSize());

        //聚合-品牌

        //获取聚合对象
        TermsAggregationBuilder trademarkAggre = AggregationBuilders.terms("tmIdAgg").field("tmId").size(10)
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName").size(10))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl").size(10));

        searchSourceBuilder.aggregation(trademarkAggre);
        //聚合-平台属性


        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg","attrs")
                .subAggregation(
                        AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                ));


        //结果集处理
        searchSourceBuilder.fetchSource(new String []{
                "id",
                "title",
                "defaultImg",
                "price"},null);


        System.out.println(searchSourceBuilder.toString());
        //设置条件构造器到请求对象
        searchRequest.source(searchSourceBuilder);

        return searchRequest;


    }
}
