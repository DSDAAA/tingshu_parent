package com.atguigu.service;

import com.atguigu.query.AlbumIndexQuery;
import com.atguigu.vo.AlbumSearchResponseVo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public interface SearchService {
    void onSaleAlbum(Long albumId);

    void offSaleAlbum(Long albumId);

    List<Map<Object, Object>> getChannelData(Long category1Id);

    AlbumSearchResponseVo search(AlbumIndexQuery albumIndexQuery);

    HashSet<String> autoCompleteSuggest(String keyword);
}
