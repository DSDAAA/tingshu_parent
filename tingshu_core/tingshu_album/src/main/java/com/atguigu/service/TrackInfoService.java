package com.atguigu.service;

import com.atguigu.entity.TrackInfo;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 声音信息 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
public interface TrackInfoService extends IService<TrackInfo> {

    void saveTrackInfo(TrackInfo trackInfo);

    void updateTrackInfoById(TrackInfo trackInfo);

    void deleteTrackInfo(Long trackId);

    IPage<TrackTempVo> getAlbumDetailTrackByPage(IPage<TrackTempVo> pageParam, Long albumId);
}
