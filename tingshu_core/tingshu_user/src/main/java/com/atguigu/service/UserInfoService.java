package com.atguigu.service;

import com.atguigu.entity.UserInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-28
 */
public interface UserInfoService extends IService<UserInfo> {

    Map<Long, Boolean> getUserShowPaidMarkOrNot(Long albumId, List<Long> needPayTrackIdList);
}
