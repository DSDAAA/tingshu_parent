package com.atguigu.service.impl;

import com.atguigu.UserFeignClient;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.TrackInfo;
import com.atguigu.entity.TrackStat;
import com.atguigu.mapper.TrackInfoMapper;
import com.atguigu.result.RetVal;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.service.TrackStatService;
import com.atguigu.service.VodService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.vo.AlbumTrackListVo;
import com.atguigu.vo.TrackTempVo;
import com.atguigu.vo.UserInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 声音信息 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Service
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {
    @Autowired
    private VodService vodService;
    @Autowired
    private AlbumInfoService albumInfoService;
    @Autowired
    private TrackStatService trackStatService;
    @Autowired
    private UserFeignClient userFeignClient;

    @Transactional
    @Override
    public void saveTrackInfo(TrackInfo trackInfo) {
        trackInfo.setUserId(AuthContextHolder.getUserId());
        trackInfo.setStatus(SystemConstant.TRACK_APPROVED);
        vodService.getTrackMediaInfo(trackInfo);
        //查询专辑中声音编号最大的值
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId, trackInfo.getAlbumId());
        wrapper.orderByAsc(TrackInfo::getOrderNum);
        wrapper.select(TrackInfo::getOrderNum);
        wrapper.last("limit 1");
        TrackInfo maxOrderNumTrackInfo = getOne(wrapper);
        int orderNum = 1;
        if (maxOrderNumTrackInfo != null) {
            orderNum = maxOrderNumTrackInfo.getOrderNum() + 1;
        }
        trackInfo.setOrderNum(orderNum);
        //保存声音
        save(trackInfo);
        //更新专辑声音个数
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        int includeTrackCount = albumInfo.getIncludeTrackCount() + 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        albumInfoService.updateById(albumInfo);
        //初始化声音的统计数据
        List<TrackStat> trackStatList = buildTrackStatData(trackInfo.getId());
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
        //更新专辑声音个数
        TrackInfo trackInfo = getById(trackId);
        AlbumInfo albumInfo = albumInfoService.getById(trackInfo.getAlbumId());
        int includeTrackCount = albumInfo.getIncludeTrackCount() - 1;
        albumInfo.setIncludeTrackCount(includeTrackCount);
        albumInfoService.updateById(albumInfo);
        removeById(trackId);
        //删除统计信息
        trackStatService.remove(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, trackId));
        //删除声音
        vodService.removeTrack(trackInfo.getMediaFileId());
    }

    @Override
    public IPage<AlbumTrackListVo> getAlbumDetailTrackByPage(IPage<AlbumTrackListVo> pageParam, Long albumId) {
        pageParam = baseMapper.getAlbumTrackAndStatInfo(pageParam, albumId);
        List<AlbumTrackListVo> albumTrackListVoList = pageParam.getRecords();
        AlbumInfo albumInfo = albumInfoService.getById(albumId);
        Long userId = AuthContextHolder.getUserId();
        if (userId == null) {
            //不是免费的
            if (SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {
                albumTrackListVoList.stream().filter(f -> f.getOrderNum().intValue() > albumInfo.getTracksForFree().intValue())
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackListVoList)) {
                    albumTrackListVoList.forEach(f -> f.setIsShowPaidMark(true));
                }
            }
        } else {
            boolean isNeedPay = false;
            if (!SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {
                UserInfoVo userInfoVo = userFeignClient.getUserById(userId).getData();
                //a,非vip用户
                if (userInfoVo.getIsVip().intValue() == 0) {
                    isNeedPay = true;
                }
                if (userInfoVo.getIsVip().intValue() == 1 && userInfoVo.getVipExpireTime().before(new Date())) {
                    isNeedPay = true;
                }
            }
            //付费
            else if (SystemConstant.NEED_PAY_ALBUM.equals(albumInfo.getPayType())) {
                isNeedPay = true;
            } else {
                isNeedPay = false;
            }
            if (isNeedPay) {
                List<AlbumTrackListVo> albumTrackNeedPayList = albumTrackListVoList.stream().filter(f -> f.getOrderNum()
                        .intValue() > albumInfo.getTracksForFree().intValue()).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackNeedPayList)) {
                    List<Long> needPayTrackIdList = albumTrackNeedPayList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
                    Map<Long, Boolean> paidMarkMap = userFeignClient.getUserShowPaidMarkOrNot(albumId, needPayTrackIdList).getData();
                    albumTrackNeedPayList.forEach(item -> {
                        item.setIsShowPaidMark(paidMarkMap.get(item.getTrackId()));
                    });

                }
            }
        }
        return pageParam;
    }

    private List<TrackStat> buildTrackStatData(Long trackId) {
        List<TrackStat> trackStatList = new ArrayList<>();
        initTrackStat(trackId, trackStatList, SystemConstant.PLAY_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.COLLECT_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.PRAISE_NUM_TRACK);
        initTrackStat(trackId, trackStatList, SystemConstant.COMMENT_NUM_TRACK);
        return trackStatList;
    }

    private static void initTrackStat(Long trackId, List<TrackStat> trackStatList, String statType) {
        TrackStat trackStat = new TrackStat();
        trackStat.setTrackId(trackId);
        trackStat.setStatType(statType);
        trackStat.setStatNum(0);
        trackStatList.add(trackStat);
    }
}
