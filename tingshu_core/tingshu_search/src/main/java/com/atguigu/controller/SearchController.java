package com.atguigu.controller;

import com.atguigu.constant.RedisConstant;
import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfoIndex;
import com.atguigu.query.AlbumIndexQuery;
import com.atguigu.result.RetVal;
import com.atguigu.service.SearchService;
import com.atguigu.vo.AlbumInfoIndexVo;
import com.atguigu.vo.AlbumSearchResponseVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    @Operation(summary = "专辑搜索") //搜索古典 西方
    @PostMapping
    public RetVal search(@RequestBody AlbumIndexQuery albumIndexQuery) {
        AlbumSearchResponseVo searchResponseVo=searchService.search(albumIndexQuery);
        return RetVal.ok(searchResponseVo);
    }
    //http://127.0.0.1/api/search/albumInfo/autoCompleteSuggest/%E5%8F%A4%E5%85%B8%E5%B0%8F
    @Operation(summary = "关键字自动补全")
    @GetMapping("autoCompleteSuggest/{keyword}")
    public RetVal autoCompleteSuggest(@PathVariable String keyword) {
        HashSet<String> suggestSet = searchService.autoCompleteSuggest(keyword);
        return RetVal.ok(suggestSet);
    }

    //http://127.0.0.1/api/search/albumInfo/getAlbumDetail/139
    /**以下内容属于专辑详情 **/
    @Operation(summary = "获取专辑详情信息")
    @GetMapping("getAlbumDetail/{albumId}")
    public RetVal getAlbumDetail(@PathVariable Long albumId) {
        Map<String, Object> retMap = searchService.getAlbumDetail(albumId);
        return RetVal.ok(retMap);
    }

    @Operation(summary = "更新排行榜列表")
    @GetMapping("updateRanking")
    public RetVal updateRanking() {
        searchService.updateRanking();
        return RetVal.ok();
    }
    @Autowired
    private RedisTemplate redisTemplate;
    @Operation(summary = "获取排行榜列表")
    @GetMapping("getRankingList/{category1Id}/{rankingType}")
    public RetVal getRankingList(@PathVariable Long category1Id,@PathVariable String rankingType) {
        List<AlbumInfoIndex> albumList = (List<AlbumInfoIndex>)redisTemplate.boundHashOps(RedisConstant.RANKING_KEY_PREFIX + category1Id).get(rankingType);
        return RetVal.ok(albumList);
    }

}
