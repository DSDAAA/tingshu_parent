package com.atguigu.mapper;

import com.atguigu.entity.TrackInfo;
import com.atguigu.query.TrackInfoQuery;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * <p>
 * 声音信息 Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {

    IPage<TrackTempVo> findUserTrackPage(IPage<TrackTempVo> pageParam, TrackInfoQuery trackInfoQuery);
}
