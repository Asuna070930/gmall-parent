package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(value = "service-user",fallback = UserDegradeFeignClient.class )
public interface UserFeignClient {

    /**
     * /api/user/inner/findUserAddressListByUserId/{userId}
     * 获取用户地址
     * @param userId
     * @return
     */
    @GetMapping("/api/user/inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable Long userId);
}
