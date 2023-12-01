package com.atguigu.controller;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.entity.UserInfo;
import com.atguigu.mapper.UserInfoMapper;
import com.atguigu.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * vip服务配置表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-12-1
 */
@RestController
@RequestMapping("/api/user/wxLogin")
public class WxLoginController {
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("wxLogin/{code}")
    public void wxLogin(@PathVariable String code) throws WxErrorException {
        //获取用户的openId
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        String openId = sessionInfo.getOpenid();
        //从数据库中查询用户信息是否存在
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserInfo::getWxOpenId, openId);
        UserInfo userInfo = userInfoService.getOne(wrapper);
        //如果认证失败 不存在 往数据库中添加
        //如果存在 往redis中添加信息
    }
}
