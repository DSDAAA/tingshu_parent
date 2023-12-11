package com.atguigu.controller;

import com.atguigu.query.AlbumIndexQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.SearchService;
import com.atguigu.vo.AlbumSearchResponseVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Tag(name = "搜索专辑管理")
@RestController
@RequestMapping("api/search/albumInfo")
public class SearchController {
    @Autowired
    private SearchService searchService;

    @Operation(summary = "上架专辑")
    @GetMapping("onSaleAlbum/{albumId}")
    public void onSaleAlbum(@PathVariable Long albumId) {
        searchService.onSaleAlbum(albumId);
    }

    @Operation(summary = "批量上架专辑")
    @GetMapping("batchOnSaleAlbum")
    public String batchOnSaleAlbum() {
        for (long i = 1; i < 1577; i++) {
            searchService.onSaleAlbum(i);
        }
        return "success";
    }

    @Operation(summary = "下架专辑")
    @GetMapping("offSaleAlbum/{albumId}")
    public void offSaleAlbum(@PathVariable Long albumId) {
        searchService.offSaleAlbum(albumId);
    }

    @Operation(summary = "获取主页频道数据")
    @GetMapping("getChannelData/{category1Id}")
    public RetVal getChannelData(@PathVariable Long category1Id) {
        List<Map<Object, Object>> channelData = searchService.getChannelData(category1Id);
        return RetVal.ok(channelData);
    }
    //http://127.0.0.1/api/search/albumInfo/autoCompleteSuggest/百家讲坛

    //http://127.0.0.1/api/search/albumInfo
    @Operation(summary = "专辑搜索")
    @PostMapping
    public RetVal search(AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo albumSearchResponseVo = searchService.search(albumIndexQuery);
        return RetVal.ok(albumSearchResponseVo);
    }

    @Operation(summary = "关键字自动补全")
    @GetMapping("autoCompleteSuggest/{keyword}")
    public RetVal autoCompleteSuggest(@PathVariable String keyword) {
        HashSet<String> suggestSet = searchService.autoCompleteSuggest(keyword);
        return RetVal.ok(suggestSet);
    }
}
