package com.atguigu.gmall.user.controller;


import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/inner")
public class UserApiController {

    @Autowired
    private UserInfoService userInfoService;

    /**
     * /api/user/inner/findUserAddressListByUserId/{userId}
     * 获取用户地址
     * @param userId
     * @return
     */
    @GetMapping("/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable Long userId){

        return userInfoService.findUserAddressListByUserId(userId);


    }
}
