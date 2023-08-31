package com.atguigu.gmall.user.mapper;

import com.atguigu.gmall.model.user.UserInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.jmx.export.annotation.ManagedAttribute;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
