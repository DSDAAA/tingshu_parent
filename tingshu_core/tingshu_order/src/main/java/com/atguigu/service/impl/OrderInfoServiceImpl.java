package com.atguigu.service.impl;

import com.atguigu.AlbumFeignClient;
import com.atguigu.UserFeignClient;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.OrderInfo;
import com.atguigu.mapper.OrderInfoMapper;
import com.atguigu.service.OrderInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.*;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 订单信息 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-15
 */
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Autowired
    private AlbumFeignClient albumFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public OrderInfoVo confirmOrder(TradeVo tradeVo) {
        Long userId = AuthContextHolder.getUserId();
        UserInfoVo userInfoVo = userFeignClient.getUserById(userId).getData();
        BigDecimal finalPrice = new BigDecimal("0");
        BigDecimal deratePrice = new BigDecimal("0.00");
        BigDecimal originalPrice = new BigDecimal("0.00");
        List<OrderDetailVo> orderDetailVoList = new ArrayList<>();
        List<OrderDerateVo> orderDerateVoList = new ArrayList<>();
        //购买整个专辑
        if (tradeVo.getItemType().equals(SystemConstant.BUY_ALBUM)) {
            AlbumInfo albumInfo = albumFeignClient.getAlbumInfoById(tradeVo.getItemId()).getData();
            originalPrice = albumInfo.getPrice();
            //如果当前用户不是VIP会员
            if (userInfoVo.getIsVip() == 0) {
                //如果专辑可以打折 -1为不打折
                if (albumInfo.getDiscount().intValue() != -1) {
                    //打折金额计算 如:100*(10-8.5)/10=15
                    deratePrice = originalPrice.multiply(new BigDecimal(10).subtract(albumInfo.getDiscount()))
                            .divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
                }
                finalPrice = originalPrice.subtract(deratePrice);
            } else {
                //如果专辑可以打折 -1为不打折
                if (albumInfo.getDiscount().intValue() != -1) {
                    //打折金额计算 如:100*(10-8.5)/10=15
                    deratePrice = originalPrice.multiply(new BigDecimal(10).subtract(albumInfo.getVipDiscount()))
                            .divide(new BigDecimal(10), 2, RoundingMode.HALF_UP);
                }
                finalPrice = originalPrice.subtract(deratePrice);
            }
            //订单明细
            OrderDetailVo orderDetailVo = new OrderDetailVo();
            orderDetailVo.setItemId(tradeVo.getItemId());
            orderDetailVo.setItemName(albumInfo.getAlbumTitle());
            orderDetailVo.setItemUrl(albumInfo.getCoverUrl());
            orderDetailVo.setItemPrice(albumInfo.getPrice());
            orderDetailVoList.add(orderDetailVo);
            //订单减免信息
            if(deratePrice.intValue()!=0){
                OrderDerateVo orderDerateVo = new OrderDerateVo();
                orderDerateVo.setDerateType(SystemConstant.ALBUM_DISCOUNT);
                orderDerateVo.setDerateAmount(deratePrice);
                orderDerateVoList.add(orderDerateVo);
            }
        }
        //购买多个声音
        //购买VIP会员
        OrderInfoVo orderInfoVo = new OrderInfoVo();
        orderInfoVo.setItemType(tradeVo.getItemType());
        orderInfoVo.setOriginalAmount(originalPrice);
        orderInfoVo.setDerateAmount(deratePrice);
        orderInfoVo.setOrderAmount(finalPrice);
        orderInfoVo.setOrderDerateVoList(orderDerateVoList);
        orderInfoVo.setOrderDetailVoList(orderDetailVoList);
        //orderInfoVo.setTimestamp(SignHelper.getTimestamp());
        orderInfoVo.setPayWay("");
        return orderInfoVo;

    }
}
