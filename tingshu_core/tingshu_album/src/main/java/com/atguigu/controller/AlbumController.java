package com.atguigu.controller;

import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.BaseAttribute;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.mapper.AlbumStatMapper;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumAttributeValueService;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumStatVo;
import com.atguigu.vo.AlbumTempVo;
import com.atguigu.vo.CategoryVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Tag(name = "专辑管理")
@RestController
@RequestMapping(value = "/api/album/albumInfo")
public class AlbumController {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @TingShuLogin
    @Operation(summary = "新增专辑")
    @PostMapping("saveAlbumInfo")
    public RetVal saveAlbumInfo(@RequestBody AlbumInfo albumInfo) {
        albumInfoService.saveAlbumInfo(albumInfo);
        return RetVal.ok();
    }

    @TingShuLogin
    @Operation(summary = "分页查询专辑")
    @PostMapping("getUserAlbumByPage/{pageNum}/{pageSize}")
    public RetVal getUserAlbumByPage(
            @Parameter(name = "pageNum", description = "当前页码", required = true)
            @PathVariable Long pageNum,
            @Parameter(name = "pageSize", description = "每页记录数", required = true)
            @PathVariable Long pageSize,
            @Parameter(name = "albumInfoQuery", description = "查询对象", required = false)
            @RequestBody AlbumInfoQuery albumInfoQuery) {
        Long userId = AuthContextHolder.getUserId();
        albumInfoQuery.setUserId(userId);
        IPage<AlbumTempVo> pageParam = new Page<>(pageNum, pageSize);
        pageParam = albumInfoMapper.getUserAlbumByPage(pageParam, albumInfoQuery);
        return RetVal.ok(pageParam);
    }

    @Operation(summary = "根据id查询专辑信息")
    @GetMapping("getAlbumInfoById/{albumId}")
    public RetVal<AlbumInfo> getAlbumInfoById(@PathVariable Long albumId) {
        AlbumInfo albumInfo = albumInfoService.getAlbumInfoById(albumId);
        return RetVal.ok(albumInfo);
    }

    @Operation(summary = "修改专辑")
    @PutMapping("updateAlbumInfo")
    public RetVal updateAlbumInfo(@RequestBody AlbumInfo albumInfo) {
        albumInfoService.updateAlbumInfo(albumInfo);
        return RetVal.ok();
    }

    @Operation(summary = "删除专辑")
    @DeleteMapping("deleteAlbumInfo/{albumId}")
    public RetVal deleteAlbumInfo(@PathVariable Long albumId) {
        albumInfoService.deleteAlbumInfo(albumId);
        return RetVal.ok();
    }

    @Autowired
    private AlbumAttributeValueService albumPropertyValueService;

    /**
     * 以下内容属于搜索板块
     **/
    @Operation(summary = "根据albumId查询专辑属性值")
    @GetMapping("getAlbumPropertyValue/{albumId}")
    public List<AlbumAttributeValue> getAlbumPropertyValue(@PathVariable Long albumId) {
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> attributeValueList = albumPropertyValueService.list(wrapper);
        return attributeValueList;
    }

    /**
     * 以下内容属于专辑详情板块
     **/
    @Autowired
    private AlbumStatMapper albumStatMapper;

    @Operation(summary = "获取专辑统计信息")
    @GetMapping("getAlbumStatInfo/{albumId}")
    public AlbumStatVo getAlbumStatInfo(@PathVariable Long albumId) {
        AlbumStatVo albumStatVo = albumStatMapper.getAlbumStatInfo(albumId);
        return albumStatVo;
    }

    //http://127.0.0.1/api/album/albumInfo/isSubscribe/139
    @TingShuLogin
    @Operation(summary = "是否订阅")
    @GetMapping("isSubscribe/{albumId}")
    public RetVal isSubscribe(@PathVariable Long albumId) {
        boolean flag = albumInfoService.isSubscribe(albumId);
        return RetVal.ok(flag);
    }

}
