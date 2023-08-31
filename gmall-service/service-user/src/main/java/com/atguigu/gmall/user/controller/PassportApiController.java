package com.atguigu.gmall.user.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportApiController {


    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * api/user/passport/login
     * 登录实现
     * @param userInfo
     * @return
     */
    @PostMapping("/login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){

        //调用service认证用户
        UserInfo user=userInfoService.login(userInfo);
        //判断
        if(user!=null){

            //生成token
            String token = UUID.randomUUID().toString().replace("-", "");
            //封装数据存储到redis
            String ip = IpUtil.getIpAddress(request);
            //存储redis  string  token --json(obj(ip userId))
            JSONObject object=new JSONObject();
            object.put("ip",ip);
            object.put("userId",user.getId().toString());
            //存储  key user:login:token
            redisTemplate.opsForValue().set(RedisConst.USER_LOGIN_KEY_PREFIX+token,object.toString(), RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);



            //封装数据响应到页面
            //页面obj( toke  nickName)
            Map<String ,String> resultMap=new HashMap<>();
            resultMap.put("token",token);
            resultMap.put("nickName",user.getNickName());

            return Result.ok(resultMap);

        }else{

            return Result.fail().message("用名或者密码错误！！！");

        }




    }


    /**
     * 用户退出
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public Result logout(HttpServletRequest request){

        String token = request.getHeader("token");
        //判断
        if(!StringUtils.isEmpty(token)){

            redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+token);

        }

        return Result.ok();

    }

}
