package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserAddressMapper;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Resource
    private UserAddressMapper userAddressMapper;
    /**
     * 用户登录
     * @param userInfo
     * @return
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        //select*from user_info where loginName=? and passwd=?

        //密码处理加密 md5
        String password = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        //条件构造器
        LambdaQueryWrapper<UserInfo> queryWrapper=new LambdaQueryWrapper();
        queryWrapper.eq(UserInfo::getLoginName,userInfo.getLoginName());
        queryWrapper.eq(UserInfo::getPasswd,password);

        //执行查询
        UserInfo user = userInfoMapper.selectOne(queryWrapper);


        return user;
    }

    /**
     * 获取用户地址
     * @param userId
     * @return
     */
    @Override
    public List<UserAddress> findUserAddressListByUserId(Long userId) {

        //select *from user_address where user_id=userId
        //封装查询条件
        QueryWrapper <UserAddress> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        //执行查询
        List<UserAddress> userAddresses = userAddressMapper.selectList(queryWrapper);

        return userAddresses;
    }
}
