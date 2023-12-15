package com.atguigu.service;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.TrackInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 专辑信息 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
public interface AlbumInfoService extends IService<AlbumInfo> {

    void saveAlbumInfo(AlbumInfo albumInfo);

    AlbumInfo getAlbumInfoById(Long albumId);

    void updateAlbumInfo(AlbumInfo albumInfo);

    void deleteAlbumInfo(Long albumId);

    boolean isSubscribe(Long albumId);
}
