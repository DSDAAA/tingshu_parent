package com.atguigu.controller;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.minio.MinioUploader;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.BaseCategory1Service;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * 一级分类表 前端控制器
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Tag(name = "并发管理接口")
@RestController
@RequestMapping(value = "/api/album")
public class ConcurentController {
    @Autowired
    private BaseCategory1Service category1Service;
    @Autowired
    private AlbumInfoService albumInfoService;

    @Operation(summary = "例子")
    @GetMapping("setNum")
    public String setNum() {
        category1Service.setNum();
        return "success";
    }

    @Autowired
    private RBloomFilter bloomFilter;

    @Operation(summary = "初始化布隆过滤器")
    @GetMapping("init")
    public String init() {
        //查询数据库里面专辑的id
        LambdaQueryWrapper<AlbumInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AlbumInfo::getId);
        List<AlbumInfo> albumInfoList = albumInfoService.list(wrapper);
        for (AlbumInfo albumInfo : albumInfoList) {
            Long albumInfoId = albumInfo.getId();
            //把id放到布隆过滤器里面
            bloomFilter.add(albumInfoId);
        }
        return "success";
    }

    @GetMapping("hello")
    public String sayHello() {
        return "success";
    }

}
