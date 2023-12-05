package com.atguigu.controller;

import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.BaseAttribute;
import com.atguigu.entity.TrackInfo;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.query.TrackInfoQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.service.VodService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumTempVo;
import com.atguigu.vo.CategoryVo;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
@RestController
@Tag(name = "声音管理")
@RequestMapping("/api/album/trackInfo")
public class TrackController {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private VodService vodService;
    @Autowired
    private TrackInfoService trackInfoService;
    @Autowired
    private TrackInfoMapper trackInfoMapper;

    @TingShuLogin(required = true)
    @Operation(summary = "根据用户ID查询用户的专辑信息")
    @GetMapping("findAlbumByUserId")
    public RetVal findAlbumByUserId() {
        Long userId = AuthContextHolder.getUserId();
        LambdaQueryWrapper<AlbumInfo> albumInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        albumInfoLambdaQueryWrapper.eq(AlbumInfo::getUserId, userId);
        albumInfoLambdaQueryWrapper.select(AlbumInfo::getId, AlbumInfo::getAlbumTitle);
        List<AlbumInfo> albumInfoList = albumInfoService.list(albumInfoLambdaQueryWrapper);
        return RetVal.ok(albumInfoList);
    }

    @Operation(summary = "上传声音")
    @PostMapping("uploadTrack")
    public RetVal uploadTrack(MultipartFile multipartFile) {
        Map<String, Object> map = vodService.iploadTrack(multipartFile);
        return RetVal.ok(map);
    }

    @Operation(summary = "新增声音")
    @PostMapping("saveTrackInfo")
    public RetVal saveTrackInfo(@RequestBody TrackInfo trackInfo) {
        trackInfoService.saveTrackInfo(trackInfo);

        return RetVal.ok();
    }

    @TingShuLogin(required = true)
    @Operation(summary = "分页查询声音")
    @PostMapping("findUserTrackPage/{pageNum}/{pageSize}")
    public RetVal findUserTrackPage(
            @PathVariable Long pageNum,
            @PathVariable Long pageSize,
            @RequestBody TrackInfoQuery trackInfoQuery) {
        Long userId = AuthContextHolder.getUserId();
        trackInfoQuery.setUserId(userId);
        IPage<TrackTempVo> pageParam = new Page<>(pageNum, pageSize);
        pageParam = trackInfoMapper.findUserTrackPage(pageParam, trackInfoQuery);
        return RetVal.ok(pageParam);
    }

    @Operation(summary = "根据id查询声音信息")
    @PostMapping("getTrackInfoById/{trackId}")
    public RetVal getTrackInfoById(@PathVariable Long trackId) {
        TrackInfo trackInfo = trackInfoService.getById(trackId);
        return RetVal.ok(trackInfo);
    }

    @Operation(summary = "修改声音")
    @PutMapping("updateTrackInfo/{albumId}")
    public RetVal updateTrackInfo(@RequestBody TrackInfo trackInfo) {
        trackInfoService.updateTrackInfoById(trackInfo);
        return RetVal.ok();
    }

    @Operation(summary = "删除声音")
    @DeleteMapping("deleteTrackInfo/{albumId}")
    public RetVal deleteTrackInfo(@PathVariable Long trackId) {
        trackInfoService.deleteTrackInfo(trackId);
        return RetVal.ok();
    }
}
