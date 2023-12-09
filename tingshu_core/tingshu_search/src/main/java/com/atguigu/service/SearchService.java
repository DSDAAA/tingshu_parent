package com.atguigu.service;

import java.util.List;
import java.util.Map;

public interface SearchService {
    void onSaleAlbum(Long albumId);

    void offSaleAlbum(Long albumId);

    List<Map<Object, Object>> getChannelData(Long category1Id);
}
