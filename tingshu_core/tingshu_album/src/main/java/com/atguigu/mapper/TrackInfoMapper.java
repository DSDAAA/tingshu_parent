package com.atguigu.mapper;

import com.atguigu.entity.TrackInfo;
import com.atguigu.query.TrackInfoQuery;
import com.atguigu.vo.TrackTempVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 声音信息 Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {

    IPage<TrackTempVo> findUserTrackPage(@Param("pageParam") IPage<TrackTempVo> pageParam, @Param("trackInfoQuery") TrackInfoQuery trackInfoQuery);
}
