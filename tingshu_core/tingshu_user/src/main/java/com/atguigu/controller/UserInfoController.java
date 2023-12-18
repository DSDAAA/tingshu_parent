package com.atguigu.controller;

import com.atguigu.entity.UserInfo;
import com.atguigu.entity.UserPaidTrack;
import com.atguigu.login.TingShuLogin;
import com.atguigu.result.RetVal;
import com.atguigu.service.UserInfoService;
import com.atguigu.service.UserPaidTrackService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Operation(summary = "获取用户是否需要购买的标识")
    @TingShuLogin
    @PostMapping("getUserShowPaidMarkOrNot/{albumId}")
    public RetVal<Map<Long, Boolean>> getUserShowPaidMarkOrNot(@PathVariable Long albumId,@RequestBody List<Long> needPayTrackIdList)  {
        Map<Long, Boolean> retMap = userInfoService.getUserShowPaidMarkOrNot(albumId, needPayTrackIdList);
        return RetVal.ok(retMap);
    }
    @Autowired
    private UserPaidTrackService userPaidTrackService;
    @Operation(summary = "获取用户支付过的声音id")
    @TingShuLogin
    @PostMapping("getPaidTrackIdList/{albumId}")
    public List<Long> getPaidTrackIdList(@PathVariable Long albumId)  {
        Long userId = AuthContextHolder.getUserId();
        LambdaQueryWrapper<UserPaidTrack> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserPaidTrack::getAlbumId,albumId);
        wrapper.eq(UserPaidTrack::getUserId,userId);
        List<UserPaidTrack> userPaidTrackList = userPaidTrackService.list(wrapper);
        List<Long> paidTrackIdList = userPaidTrackList.stream().map(UserPaidTrack::getTrackId).collect(Collectors.toList());
        return paidTrackIdList;
    }
}
