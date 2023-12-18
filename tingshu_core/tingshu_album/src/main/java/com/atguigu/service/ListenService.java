package com.atguigu.service;

import com.atguigu.vo.UserCollectVo;
import com.atguigu.vo.UserListenProcessVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Map;

public interface ListenService {
    Map<String, Object> getRecentlyPlay();

    void updatePlaySecond(UserListenProcessVo userListenProcessVo);

    BigDecimal getLastPlaySecond(Long trackId);

    boolean collectTrack(Long trackId);

    boolean isCollect(Long trackId);

    IPage<UserCollectVo> getUserCollectByPage(Integer pageNum, Integer pageSize);

    IPage getPlayHistoryTrackByPage(Integer pageNum, Integer pageSize);
}
