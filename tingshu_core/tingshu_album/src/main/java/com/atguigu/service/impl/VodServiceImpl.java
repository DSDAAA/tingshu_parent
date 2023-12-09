package com.atguigu.service.impl;

import com.atguigu.config.VodProperties;
import com.atguigu.entity.TrackInfo;
import com.atguigu.service.VodService;
import com.atguigu.util.UploadFileUtil;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.vod.v20180717.VodClient;
import com.tencentcloudapi.vod.v20180717.models.*;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
@Service
public class VodServiceImpl implements VodService {
    @Autowired
    private VodProperties vodProperties;

    @SneakyThrows
    @Override
    public Map<String, Object> uploadTrack(MultipartFile file) {
        //声音上传的临时文件
        String tempPath = UploadFileUtil.uploadTempPath(vodProperties.getTempPath(), file);
        VodUploadClient client = new VodUploadClient(vodProperties.getSecretId(),
                vodProperties.getSecretKey());
        VodUploadRequest request = new VodUploadRequest();
        request.setMediaFilePath(tempPath);
        VodUploadResponse response = client.upload(vodProperties.getRegion(), request);
        Map<String, Object> map = new HashMap<>();
        map.put("mediaFileId",response.getFileId());
        map.put("mediaUrl",response.getMediaUrl());
        return map;
    }

    @SneakyThrows
    @Override
    public void getTrackMediaInfo(TrackInfo trackInfo) {
        Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());
        // 实例化要请求产品的client对象,clientProfile是可选的
        VodClient client = new VodClient(cred, vodProperties.getRegion());
        // 实例化一个请求对象,每个接口都会对应一个request对象
        DescribeMediaInfosRequest req = new DescribeMediaInfosRequest();
        String[] mediaFileIds = {trackInfo.getMediaFileId()};
        req.setFileIds(mediaFileIds);
        // 返回的resp是一个DescribeMediaInfosResponse的实例，与请求对象对应
        DescribeMediaInfosResponse resp = client.DescribeMediaInfos(req);
       if(resp.getMediaInfoSet().length>0){
           MediaInfo mediaInfo = resp.getMediaInfoSet()[0];
           trackInfo.setMediaSize(mediaInfo.getMetaData().getSize());
           trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfo.getMetaData().getDuration()));
           trackInfo.setMediaType(mediaInfo.getBasicInfo().getType());
       }
    }

    @SneakyThrows
    @Override
    public void removeTrack(String mediaFileId) {
        Credential cred = new Credential(vodProperties.getSecretId(), vodProperties.getSecretKey());
        VodClient client = new VodClient(cred,  vodProperties.getRegion());
        DeleteMediaRequest req = new DeleteMediaRequest();
        req.setFileId(mediaFileId);
        client.DeleteMedia(req);
    }
}
