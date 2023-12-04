package com.atguigu.mapper;

import com.atguigu.entity.AlbumInfo;
import com.atguigu.query.AlbumInfoQuery;
import com.atguigu.vo.AlbumTempVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * <p>
 * 专辑信息 Mapper 接口
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
public interface AlbumInfoMapper extends BaseMapper<AlbumInfo> {

    IPage<AlbumTempVo> getUserAlbumByPage(IPage<AlbumTempVo> pageParam, AlbumInfoQuery albumInfoQuery);
}
