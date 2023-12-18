package com.atguigu.service;

import com.atguigu.entity.OrderInfo;
import com.atguigu.vo.OrderInfoVo;
import com.atguigu.vo.TradeVo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 订单信息 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-15
 */
public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfoVo confirmOrder(TradeVo tradeVo);
}
