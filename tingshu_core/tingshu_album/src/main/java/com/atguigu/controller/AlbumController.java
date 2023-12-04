package com.atguigu.controller;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.BaseAttribute;
import com.atguigu.login.TingShuLogin;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.mapper.BaseAttributeMapper;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategoryViewService;
import com.atguigu.vo.AlbumTempVo;
import com.atguigu.vo.CategoryVo;
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
 * @since 2023-12-01
 */
@RestController
@Tag(name = "专辑管理")
@RequestMapping("/api/album/albumInfo")
public class AlbumController {
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private AlbumInfoMapper albumInfoMapper;

    @TingShuLogin(required = true)
    @Operation(summary = "新增专辑")
    @PostMapping("getAllCategoryList")
    public RetVal getAllCategoryList(@RequestBody AlbumInfo albumInfo) {
        albumInfoService.saveAlbumInfo(albumInfo);
        return RetVal.ok();
    }

    @Operation(summary = "分页查询专辑")
    @PostMapping("getUserAlbumByPage/{pageNum}/{pageSize}")
    public RetVal getUserAlbumByPage(@Parameter(name = "pageNum", description = "当前页码", required = true)
                                     @PathVariable Long pageNum,
                                     @Parameter(name = "pageSize", description = "每页记录数", required = true)
                                     @PathVariable Long pageSize,
                                     @Parameter(name = "albumInfoQuery", description = "查询对象", required = false)
                                     AlbumInfoQuery albumInfoQuery) {
        IPage<AlbumTempVo> pageParam = new Page<>(pageNum, pageSize);
        albumInfoMapper.getUserAlbumByPage(pageParam);
        return RetVal.ok();
    }
}
