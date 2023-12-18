package com.atguigu.controller;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.service.ListenService;
import com.atguigu.vo.CategoryVo;
import com.atguigu.vo.TrackStatVo;
import com.atguigu.vo.UserCollectVo;
import com.atguigu.vo.UserListenProcessVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Tag(name = "听专辑管理接口")
@RestController
@RequestMapping(value = "/api/album/category")
public class ListenController {
    @Autowired
    private ListenService listenService;

    @TingShuLogin
    @Operation(summary = "最近播放")
    @GetMapping("getRecentlyPlay")
    public RetVal getRecentlyPlay() {
        Map<String, Object> recentlyPlay = listenService.getRecentlyPlay();
        return RetVal.ok(recentlyPlay);
    }

    @TingShuLogin
    @Operation(summary = "更新播放时间")
    @PostMapping("updatePlaySecond")
    public RetVal updatePlaySecond(@RequestBody UserListenProcessVo userListenProcessVo) {
        listenService.updatePlaySecond(userListenProcessVo);
        return RetVal.ok();
    }

    @TingShuLogin
    @Operation(summary = "最近播放")
    @GetMapping("getLastPlaySecond/{trackId}")
    public RetVal getLastPlaySecond(@PathVariable Long trackId) {
        BigDecimal second = listenService.getLastPlaySecond(trackId);
        return RetVal.ok(second);
    }

    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @Operation(summary = "获取声音的统计信息")
    @GetMapping("getTrackStatistics/{trackId}")
    public RetVal getTrackStatistics(@PathVariable Long trackId) {
        TrackStatVo trackStatVo = trackInfoMapper.getTrackStatistics(trackId);
        return RetVal.ok(trackStatVo);
    }

    @TingShuLogin
    @Operation(summary = "获取声音的统计信息")
    @GetMapping("collectTrack/{trackId}")
    public RetVal collectTrack(@PathVariable Long trackId) {
        boolean flag = listenService.collectTrack(trackId);
        return RetVal.ok(flag);
    }

    @TingShuLogin
    @Operation(summary = "是否收藏过")
    @GetMapping("isCollect/{trackId}")
    public RetVal isCollect(@PathVariable Long trackId) {
        boolean flag = listenService.isCollect(trackId);
        return RetVal.ok(flag);
    }

    @TingShuLogin
    @Operation(summary = "是否收藏过")
    @GetMapping("getUserCollectByPage/{pageNum}/{pageSize}")
    public RetVal getUserCollectByPage(@PathVariable Integer pageNum, @PathVariable Integer pageSize) {
        IPage<UserCollectVo> pageParam = listenService.getUserCollectByPage(pageNum, pageSize);
        return RetVal.ok(pageParam);
    }
    @TingShuLogin
    @Operation(summary = "获取用户声音历史播放列表")
    @GetMapping("getPlayHistoryTrackByPage/{pageNum}/{pageSize}")
    public RetVal getPlayHistoryTrackByPage(@PathVariable Integer pageNum, @PathVariable Integer pageSize) {
        IPage pageParam = listenService.getPlayHistoryTrackByPage(pageNum, pageSize);
        return RetVal.ok(pageParam);
    }
}
