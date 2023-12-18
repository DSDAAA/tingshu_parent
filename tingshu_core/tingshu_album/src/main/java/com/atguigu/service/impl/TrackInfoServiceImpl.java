package com.atguigu.service.impl;

import com.atguigu.UserFeignClient;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.TrackInfo;
import com.atguigu.entity.TrackStat;
import com.atguigu.mapper.TrackInfoMapper;
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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
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

    @Autowired
    private UserFeignClient userFeignClient;

    @Override
    public IPage<AlbumTrackListVo> getAlbumDetailTrackByPage(IPage<AlbumTrackListVo> pageParam, Long albumId) {
        pageParam = baseMapper.getAlbumTrackAndStatInfo(pageParam, albumId);
        List<AlbumTrackListVo> albumTrackVoList = pageParam.getRecords();
        AlbumInfo albumInfo = albumInfoService.getById(albumId);
        Long userId = AuthContextHolder.getUserId();
        //如果用户没有登录
        if (userId == null) {
            //不是免费的
            if (!SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {
                //获取付费的声音列表
                List<AlbumTrackListVo> albumTrackNeedPayList = albumTrackVoList.stream().filter(f -> f.getOrderNum()
                                .intValue() > albumInfo.getTracksForFree().intValue())
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(albumTrackNeedPayList)) {
                    albumTrackNeedPayList.forEach(f -> f.setIsShowPaidMark(true));
                }
            }
        } else {
            boolean isNeedPay = false;
            //vip免费
            if (SystemConstant.VIPFREE_ALBUM.equals(albumInfo.getPayType())) {
                UserInfoVo userInfoVo = userFeignClient.getUserById(userId).getData();
                //a.非vip用户
                if (userInfoVo.getIsVip().intValue() == 0) {
                    isNeedPay = true;
                }
                //b.是vip用户 但是vip过期
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
            //需要付费，判断用户是否购买过专辑或声音
            if (isNeedPay) {
                List<AlbumTrackListVo> albumTrackNeedPayList = albumTrackVoList.stream().filter(f -> f.getOrderNum()
                                .intValue() > albumInfo.getTracksForFree().intValue()).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(albumTrackNeedPayList)){
                    //拿到需要付费专辑的id
                    List<Long> needPayTrackIdList = albumTrackNeedPayList.stream().map(AlbumTrackListVo::getTrackId).collect(Collectors.toList());
                    Map<Long, Boolean> paidMarkMap = userFeignClient.getUserShowPaidMarkOrNot(albumId, needPayTrackIdList).getData();
                    albumTrackNeedPayList.forEach(item->{
                        item.setIsShowPaidMark(paidMarkMap.get(item.getTrackId()));
                    });

                }
            }
        }
        return pageParam;
    }

    @Override
    public List<TrackTempVo> getTrackVoList(List<Long> trackIdList) {
        List<TrackInfo> trackInfoList = listByIds(trackIdList);
        return trackInfoList.stream().map(trackInfo->{
            TrackTempVo trackTempVo = new TrackTempVo();
            BeanUtils.copyProperties(trackInfo,trackTempVo);
            trackTempVo.setTrackId(trackInfo.getId());
            return trackTempVo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTrackListToChoose(Long trackId) {
        //获取声音信息
        TrackInfo trackInfo = getById(trackId);
        //获取专辑信息
        Long albumId = trackInfo.getAlbumId();
        AlbumInfo albumInfo = albumInfoService.getById(albumId);
        //获取用户已经购买过的声音
        List<Long> paidTrackIdList = userFeignClient.getPaidTrackIdList(albumId);
        //获取比当前声音编号大的声音信息
        LambdaQueryWrapper<TrackInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TrackInfo::getAlbumId,albumId);
        wrapper.gt(TrackInfo::getOrderNum,trackInfo.getOrderNum());
        wrapper.select(TrackInfo::getId);
        List<TrackInfo> trackInfoList = list(wrapper);
        List<Long> trackIdList = trackInfoList.stream().map(TrackInfo::getId).collect(Collectors.toList());
        //未支付的声音id
        List<Long> noPayTrackIdList = new ArrayList<>();
        if(CollectionUtils.isEmpty(paidTrackIdList)){
            noPayTrackIdList=trackIdList;
        }else{
            noPayTrackIdList=trackIdList.stream().filter(tempTrackId->!paidTrackIdList
                     .contains(tempTrackId)).collect(Collectors.toList());
        }
        List<Map<String, Object>> list = new ArrayList<>();
        //本集---写了一个最low的版本 作业---你自己优化
        if(noPayTrackIdList.size()>=0){
            Map<String, Object> map=new HashMap<>();
            map.put("name", "本集");
            map.put("price", albumInfo.getPrice());
            map.put("trackCount", 0);
            list.add(map);
        }
        //后多少集
        if(noPayTrackIdList.size()>0&&noPayTrackIdList.size()<=10){
            Map<String, Object> map=new HashMap<>();
            int count = noPayTrackIdList.size();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(count));
            map.put("name", "后"+count+"集");
            map.put("price", price);
            map.put("trackCount", count);
            list.add(map);
        }
        if (noPayTrackIdList.size() > 10) {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(10));
            map.put("name", "后10集");
            map.put("price", price);
            map.put("trackCount", 10);
            list.add(map);
        }

        //后20集
        if (noPayTrackIdList.size() > 10 && noPayTrackIdList.size() <= 20) {
            Map<String, Object> map = new HashMap<>();
            int count = noPayTrackIdList.size();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(count));
            map.put("name", "后" + count + "集");
            map.put("price", price);
            map.put("trackCount", count);
            list.add(map);
        }
        if (noPayTrackIdList.size() > 20) {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(20));
            map.put("name", "后20集");
            map.put("price", price);
            map.put("trackCount", 20);
            list.add(map);
        }

        //后30集
        if (noPayTrackIdList.size() > 20 && noPayTrackIdList.size() <= 30) {
            Map<String, Object> map = new HashMap<>();
            int count = noPayTrackIdList.size();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(count));
            map.put("name", "后" + count + "集");
            map.put("price", price);
            map.put("trackCount", count);
            list.add(map);
        }
        if (noPayTrackIdList.size() > 30) {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(30));
            map.put("name", "后30集");
            map.put("price", price);
            map.put("trackCount", 30);
            list.add(map);
        }

        //后50集
        if (noPayTrackIdList.size() > 30 && noPayTrackIdList.size() <= 50) {
            Map<String, Object> map = new HashMap<>();
            int count = noPayTrackIdList.size();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(count));
            map.put("name", "后" + count + "集");
            map.put("price", price);
            map.put("trackCount", count);
            list.add(map);
        }
        if (noPayTrackIdList.size() > 50) {
            Map<String, Object> map = new HashMap<>();
            BigDecimal price = albumInfo.getPrice().multiply(new BigDecimal(50));
            map.put("name", "后50集");
            map.put("price", price);
            map.put("trackCount", 50);
            list.add(map);
        }
        return list;
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
