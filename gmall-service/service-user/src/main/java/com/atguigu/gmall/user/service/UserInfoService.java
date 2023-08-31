package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;

import java.util.List;

public interface UserInfoService {
    /**
     * 用户登录
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 获取用户地址
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(Long userId);
}
