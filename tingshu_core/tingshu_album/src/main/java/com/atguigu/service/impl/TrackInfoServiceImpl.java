package com.atguigu.service.impl;

import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.AlbumStat;
import com.atguigu.entity.TrackInfo;
import com.atguigu.entity.TrackStat;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.service.TrackStatService;
import com.atguigu.service.VodService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 声音信息 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
@Service
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {
    @Autowired
    private VodService vodService;
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private TrackStatService trackStatService;

    @Override
    public void saveTrackInfo(TrackInfo trackInfo) {
        trackInfo.setUserId(AuthContextHolder.getUserId());
        trackInfo.setStatus(SystemConstant.TRACK_APPROVED);
        vodService.getTrackMediaInfo(trackInfo);
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
        wrapper.orderByAsc(TrackInfo::getOrderNum);
        wrapper.select(TrackInfo::getOrderNum);
        wrapper.last("limit 1");
        TrackInfo maxOrderNumberTrackInfo = getOne(wrapper);
        int orderNum = 1;
        if (maxOrderNumberTrackInfo != null) {
            orderNum = maxOrderNumberTrackInfo.getOrderNum() + 1;
        }
        trackInfo.setOrderNum(orderNum);
        //保存声音
        save(trackInfo);
        //更新专辑声音个数
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        int includeTrackCount = albumInfo.getIncludeTrackCount() + 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        boolean b = albumInfoService.updateById(albumInfo);
        //声音的初始化统计数据
        List<TrackStat> trackStatList = buildAlbumStatData(trackInfo.getId());
        trackStatService.saveBatch(trackStatList);
    }

    @Override
    public void updateTrackInfoById(TrackInfo trackInfo) {
        vodService.getTrackMediaInfo(trackInfo);
        updateById(trackInfo);
    }

    @Transactional
    @Override
    public void deleteTrackInfo(Long trackId) {
        TrackInfo trackInfo = getById(trackId);
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        int includeTrackCount = albumInfo.getIncludeTrackCount() + 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        albumInfoService.updateById(albumInfo);
        removeById(trackId);
        //删除统计信息
        trackStatService.remove(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
        //删除声音
        vodService.removeTrack(trackInfo.getMediaFileId());
    }

    private List<TrackStat> buildAlbumStatData(Long albumId) {
        ArrayList<TrackStat> trackStatList = new ArrayList<>();
        initTrackStat(albumId, trackStatList, SystemConstant.PLAY_NUM_TRACK);
        initTrackStat(albumId, trackStatList, SystemConstant.COLLECT_NUM_TRACK);
        initTrackStat(albumId, trackStatList, SystemConstant.PRAISE_NUM_TRACK);
        initTrackStat(albumId, trackStatList, SystemConstant.COMMENT_NUM_TRACK);
        return trackStatList;
    }

    private static void initTrackStat(Long trackId, ArrayList<TrackStat> trackStatArrayList, String statType) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(0);
        trackStatArrayList.add(trackStat);
    }
}
