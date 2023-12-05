package com.atguigu.service;

import com.atguigu.entity.TrackInfo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * <p>
 * 声音统计 服务类
 * </p>
 *
 * @author Dunston
 * @since 2023-12-01
 */
public interface VodService {

    Map<String, Object> iploadTrack(MultipartFile multipartFile);

    void getTrackMediaInfo(TrackInfo trackInfo);

    void removeTrack(String mediaFileId);
}
