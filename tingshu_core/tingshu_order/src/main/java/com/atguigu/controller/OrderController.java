package com.atguigu.controller;

import com.atguigu.entity.UserInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.result.RetVal;
import com.atguigu.service.OrderInfoService;
import com.atguigu.vo.OrderInfoVo;
import com.atguigu.vo.TradeVo;
import com.atguigu.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 订单信息 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-12-15
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("api/order/orderInfo")
public class OrderController {
    @Autowired
    private OrderInfoService orderInfoService;
    @Operation(summary = "确认订单")
    @PostMapping("confirmOrder")
    @TingShuLogin
    public RetVal confirmOrder(@RequestBody TradeVo tradeVo)  {
        OrderInfoVo orderInfoVo = orderInfoService.confirmOrder(tradeVo);
        return RetVal.ok(orderInfoVo);
    }

}
