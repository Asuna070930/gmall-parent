package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGlobalFilter implements GlobalFilter {

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${authUrls.url}")
    private String authUrls;

    private AntPathMatcher antPathMatcher=new AntPathMatcher();
    /**
     * 全局认证过滤器
     *
     * 用户认证思路：
     *  1.获取用户请求的path
     *  2.判断一：是否为内部路径
     *   /星星 /inner /星星--拒绝
     *
     *   String userId=//用户认证
     *
     *  3.判断二：是否已经登录
     *   /星星/auth/星星 --响应没有权限
     *
     *  4.白名单  需要拦截web同步资源
     *   order.html pay.html cart.html  --重定向到登录
     *
     *  5.放行
     * @param exchange the current server exchange
     * @param chain provides a way to delegate to the next filter
     * @return
     *
     * path演示：
     *  /admin/product/baseTrademark/get/61
     *  /api/product/inner/findSpuPosterBySpuId/8
     *  /26.html
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取响应对象
        ServerHttpResponse response = exchange.getResponse();
        //获取path
        String path = request.getURI().getPath();
        System.out.println(path);

        //判断一：内部资源 /**/inner/**
        if(antPathMatcher.match("/**/inner/**",path)){

           return  this.out(response, ResultCodeEnum.PERMISSION);

        }

        //认证用户-获取用户名--redis--token
        String userId=this.getUserId(request);

        //ip的被盗用的判断
        if("-1".equals(userId)){

            return this.out(response, ResultCodeEnum.ILLEGAL_REQUEST);

        }

        //判断二：需要认证的资源，否在拦截 /**/auth/**
        if(antPathMatcher.match("/**/auth/**",path) && StringUtils.isEmpty(userId)){

            return this.out(response,ResultCodeEnum.LOGIN_AUTH);


        }
        //判断三：白名单
        if(!StringUtils.isEmpty(authUrls)){
            String[] auths = authUrls.split(",");
            //遍历
            for (String auth : auths) {//auth trade.html path--/trade.html
                //判断
                if(path.indexOf(auth)!=-1&&StringUtils.isEmpty(userId)){

                    //重定向
                    //设置状态码
                    response.setStatusCode(HttpStatus.SEE_OTHER);

                    //设置location头

                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());

                    //响应
                    return response.setComplete();


                }

            }



        }

        //获取临时用户id
        String userTempId=this.getUserTempId(request);



        //携带userId
        if(!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){

            if(!StringUtils.isEmpty(userId)){

                request.mutate().header("userId",userId).build();
            }
            if(!StringUtils.isEmpty(userTempId)){

                request.mutate().header("userTempId",userTempId).build();
            }
            //携带参数
            return chain.filter(exchange.mutate().request(request).build());
        }


        //放行
        return chain.filter(exchange);
    }

    /**
     * 获取临时用户id
     * @param request
     * @return
     */
    private String getUserTempId(ServerHttpRequest request) {

        //从hear中获取
        String userTempId = request.getHeaders().getFirst("userTempId");
        //来判断
        if(StringUtils.isEmpty(userTempId)){
           //从cookie中
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //判断
            if(cookies!=null){
                HttpCookie cookie = cookies.getFirst("userTempId");
                //判断
                if(cookie!=null){
                    userTempId=cookie.getValue();

                }
            }


        }

        return userTempId;
    }


    /**
     * 获取用户名
     * @param request
     * @return
     *
     * 1.获取成功
     *  userId
     * 2.获取失败
     *   “”
     * 3.获取了userId,但是ip不对，有可能token被盗用
     *  -1
     *
     *
     */
    private String getUserId(ServerHttpRequest request) {

        //获取token
        //先从header中获取
        String token = request.getHeaders().getFirst("token");
        //判断
        if(StringUtils.isEmpty(token)){
            //header中没有，尝试从cookie获取
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //判断
            if(cookies!=null){
                HttpCookie cookie = cookies.getFirst("token");
                //判断
                if(cookie!=null){
                    token=cookie.getValue();

                }


            }


        }

        //判断token
        //判断
        if(!StringUtils.isEmpty(token)){
            //获取userId
            String strJson = (String) redisTemplate.opsForValue().get("user:login:" + token);
            //判断
            if(!StringUtils.isEmpty(strJson)){

                //转换
                JSONObject jsonObject = JSONObject.parseObject(strJson);
                //获取userId
                String userId = jsonObject.getString("userId");
                //获取ip
                String ip = jsonObject.getString("ip");
                //获取当前ip
                String ipAddress = IpUtil.getGatwayIpAddress(request);
                if(ipAddress.equals(ip)){
                    return userId;
                }else{
                    return "-1";
                }

            }


        }
        return "";
    }

    /**
     * 响应内容
     *
     * @param response
     * @param permission
     * @return
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {

        //封装响应对象
        Result<Object> result = Result.build(null, permission);
        //转换成json字符串
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);

        //获取DataBuffer
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //设置中文乱码
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        return response.writeWith(Mono.just(wrap));
    }


}
