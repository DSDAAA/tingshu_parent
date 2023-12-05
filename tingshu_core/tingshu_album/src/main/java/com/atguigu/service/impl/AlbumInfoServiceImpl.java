package com.atguigu.service.impl;

import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.AlbumStat;
import com.atguigu.entity.TrackInfo;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.service.AlbumAttributeValueService;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.AlbumStatService;
import com.atguigu.util.AuthContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 专辑信息 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
@Service
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private AlbumStatService albumStatService;

    @Transactional
    @Override
    public void saveAlbumInfo(AlbumInfo albumInfo) {
        Long userId = AuthContextHolder.getUserId();
        albumInfo.setUserId(userId);
        albumInfo.setStatus(SystemConstant.ALBUM_APPROVED);
        //付费专辑前5集免费
        if (!SystemConstant.FREE_ALBUM.equals(albumInfo.getPayType())) {
            albumInfo.setTracksForFree(5);
        }
        save(albumInfo);
        //保存专辑标签属性
        List<AlbumAttributeValue> albumPropertyValueList = albumInfo.getAlbumPropertyValueList();
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            for (AlbumAttributeValue albumAttributeValue : albumPropertyValueList) {
                //设置专辑id 是否能拿到？
                albumAttributeValue.setAlbumId(albumInfo.getId());
//                albumAttributeValueService.save(albumAttributeValue);
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //保存专辑的统计信息
        List<AlbumStat> albumStatsList = buildAlbumStatData(albumInfo.getId());
        albumStatService.saveBatch(albumStatsList);
        //TODO 后面再说
    }

    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        AlbumInfo albumInfo = getById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> albumInfoLambdaQueryWrapper = new LambdaQueryWrapper<>();
        albumInfoLambdaQueryWrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(albumInfoLambdaQueryWrapper);
        albumInfo.setAlbumPropertyValueList(albumAttributeValueList);
        return null;
    }

    @Override
    public void updateAlbumInfo(AlbumInfo albumInfo) {
        //修改专辑基本信息
        updateById(albumInfo);
        //修改专辑标签属性信息
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumInfo.getId());
        albumAttributeValueService.remove(wrapper);
        //保存专辑标签属性
        List<AlbumAttributeValue> albumPropertyValueList = albumInfo.getAlbumPropertyValueList();
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            for (AlbumAttributeValue albumAttributeValue : albumPropertyValueList) {
                //设置专辑id
                albumAttributeValue.setAlbumId(albumInfo.getId());
//                albumAttributeValueService.save(albumAttributeValue);
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //TODO 还有其他事情要做
    }

    @Override
    public void deleteAlbumInfo(Long albumId) {
        removeById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        albumAttributeValueService.remove(wrapper);
        albumStatService.remove(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        //TODO 还有其他事情要做
    }

    private List<AlbumStat> buildAlbumStatData(Long albumId) {
        ArrayList<AlbumStat> albumStatList = new ArrayList<>();
        initAlbumStat(albumId, albumStatList, SystemConstant.PLAY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.SUBSCRIBE_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.BUY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.COMMENT_NUM_ALBUM);
        return albumStatList;
    }

    private static void initAlbumStat(Long albumId, ArrayList<AlbumStat> albumStatArrayList, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatArrayList.add(albumStat);
    }
}
