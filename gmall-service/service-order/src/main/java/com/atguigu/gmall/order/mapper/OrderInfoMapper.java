package com.atguigu.gmall.order.mapper;

import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
    /**
     * 我的订单
     * @param userId
     * @param orderInfoPage
     * @return
     */
    IPage<OrderInfo> selectOrdersByUserId(String userId, Page<OrderInfo> orderInfoPage);
}
