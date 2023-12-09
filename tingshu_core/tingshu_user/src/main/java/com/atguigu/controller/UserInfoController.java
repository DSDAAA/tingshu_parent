package com.atguigu.controller;

import com.atguigu.entity.UserInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.UserInfoVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-11-28
 */
@Tag(name = "用户管理接口")
@RestController
@RequestMapping("api/user/userInfo")
public class UserInfoController {
    @Autowired
    private UserInfoService userInfoService;
    @Operation(summary = "获取用户个人信息")
    @GetMapping("getUserById/{userId}")
    public RetVal<UserInfoVo> getUserById(@PathVariable Long userId)  {
        UserInfo userInfo = userInfoService.getById(userId);
        UserInfoVo userInfoVo = new UserInfoVo();
        if(userInfo!=null){
            BeanUtils.copyProperties(userInfo,userInfoVo);
        }
        return RetVal.ok(userInfoVo);
    }

}
