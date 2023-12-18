package com.atguigu.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.fastjson.JSON;
import com.atguigu.constant.KafkaConstant;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.UserCollect;
import com.atguigu.entity.UserListenProcess;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.KafkaService;
import com.atguigu.service.ListenService;
import com.atguigu.service.TrackInfoService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MongoUtil;
import com.atguigu.vo.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.bson.types.ObjectId;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
public class ListenServiceImpl implements ListenService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Map<String, Object> getRecentlyPlay() {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId));
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        query.with(sort);
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("albumId", userListenProcess.getAlbumId());
        map.put("trackId", userListenProcess.getTrackId());
        return map;
    }

    @Override
    public void updatePlaySecond(UserListenProcessVo userListenProcessVo) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(userListenProcessVo.getTrackId()));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));

        if (userListenProcess == null) {
            //把播放进度保存起来
            userListenProcess = new UserListenProcess();
            BeanUtils.copyProperties(userListenProcessVo, userListenProcess);
            userListenProcess.setId(ObjectId.get().toString());
            userListenProcess.setUserId(userId);
            userListenProcess.setIsShow(1);
            userListenProcess.setCreateTime(new Date());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        } else {
            //更新mongobd中的播放记录
            userListenProcess.setBreakSecond(userListenProcessVo.getBreakSecond());
            userListenProcess.setUpdateTime(new Date());
            mongoTemplate.save(userListenProcess, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        }
        //更新播放次数
        String key = "user:track:" + new DateTime().toString("yyyyMMdd") + ":" + userListenProcessVo.getTrackId();
        Boolean isExists = redisTemplate.opsForValue().getBit(key, userId);
        if (!isExists) {
            redisTemplate.opsForValue().setBit(key, userId, true);
            //设置一天后过期
            //LocalDateTime localDateTime = LocalDateTime.now().plusDays(1);
            redisTemplate.expire(key, 1, TimeUnit.DAYS);
            //发送消息增加播放量
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatMqVo.setAlbumId(userListenProcessVo.getAlbumId());
            trackStatMqVo.setTarckId(userListenProcessVo.getTrackId());
            trackStatMqVo.setStatType(SystemConstant.PLAY_NUM_TRACK);
            trackStatMqVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatMqVo));
        }
    }

    @Override
    public BigDecimal getLastPlaySecond(Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        UserListenProcess userListenProcess = mongoTemplate.findOne(query, UserListenProcess.class,
                MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        if (userListenProcess != null) {
            return userListenProcess.getBreakSecond();
        }
        return new BigDecimal(0);
    }

    @Override
    public boolean collectTrack(Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        long count = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        if (count == 0) {
            UserCollect userCollect = new UserCollect();
            userCollect.setId(ObjectId.get().toString());
            userCollect.setUserId(userId);
            userCollect.setTrackId(trackId);
            userCollect.setCreateTime(new Date());
            mongoTemplate.save(userCollect, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
            //更新声音点赞量
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatMqVo.setTarckId(trackId);
            trackStatMqVo.setStatType(SystemConstant.COLLECT_NUM_TRACK);
            trackStatMqVo.setCount(1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatMqVo));
            return true;
        } else {
            mongoTemplate.remove(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
            TrackStatMqVo trackStatMqVo = new TrackStatMqVo();
            trackStatMqVo.setBusinessNo(UUID.randomUUID().toString().replaceAll("-", ""));
            trackStatMqVo.setTarckId(trackId);
            trackStatMqVo.setStatType(SystemConstant.COLLECT_NUM_TRACK);
            trackStatMqVo.setCount(-1);
            kafkaService.sendMessage(KafkaConstant.UPDATE_TRACK_STAT_QUEUE, JSON.toJSONString(trackStatMqVo));
            return false;
        }
    }

    @Override
    public boolean isCollect(Long trackId) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("trackId").is(trackId));
        long count = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Autowired
    private TrackInfoService trackInfoService;

    @Override
    public IPage<UserCollectVo> getUserCollectByPage(Integer pageNum, Integer pageSize) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId));
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
        query.with(pageRequest);
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        query.with(sort);
        List<UserCollect> userCollectList = mongoTemplate.find(query, UserCollect.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        long total = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_COLLECT, userId));
        List<Long> trackIdList = userCollectList.stream().map(UserCollect::getTrackId).collect(Collectors.toList());
        List<UserCollectVo> userCollectVoList = null;
        if (!CollectionUtils.isEmpty(trackIdList)) {
            List<TrackTempVo> trackVoList = trackInfoService.getTrackVoList(trackIdList);
            Map<Long, TrackTempVo> trackTempVoMap = trackVoList.stream().collect(Collectors.toMap(TrackTempVo::getTrackId, trackVo -> trackVo));
            userCollectVoList = userCollectList.stream().map(userCollect -> {
                UserCollectVo userCollectVo = new UserCollectVo();
                Long trackId = userCollect.getTrackId();
                TrackTempVo trackTempVo = trackTempVoMap.get(trackId);
                BeanUtils.copyProperties(trackTempVo, userCollectVo);
                userCollectVo.setTrackId(trackId);
                userCollectVo.setCreateTime(userCollect.getCreateTime());
                return userCollectVo;
            }).collect(Collectors.toList());
        }

        return new Page(pageNum, pageSize, total).setRecords(userCollectVoList);
    }

    @Autowired
    private AlbumInfoService albumInfoService;

    @Override
    public IPage getPlayHistoryTrackByPage(Integer pageNum, Integer pageSize) {
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId));
        PageRequest pageRequest = PageRequest.of(pageNum, pageSize);
        query.with(pageRequest);
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        query.with(sort);
        List<UserListenProcess> userListenProcessList = mongoTemplate.find(query, UserListenProcess.class, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        long total = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_LISTEN_PROCESS, userId));
        List<UserCollectVo> userCollectVoList = null;
        List<Long> albumIdList = userListenProcessList.stream().map(UserListenProcess::getAlbumId).collect(Collectors.toList());
        List<AlbumTempVo> albumTempList = albumInfoService.getAlbumTempList(albumIdList);
        Map<Long, AlbumTempVo> albumTempVoMap = albumTempList.stream().collect(Collectors.toMap(AlbumTempVo::getAlbumId, albumTempVo -> albumTempVo));
        List<Long> collect = userListenProcessList.stream().map(UserListenProcess::getTrackId).collect(Collectors.toList());
        List<TrackTempVo> trackTempVoList = trackInfoService.getTrackVoList(collect);
        Map<Long, TrackTempVo> trackTempVoMap = trackTempVoList.stream().collect(Collectors.toMap(TrackTempVo::getTrackId, trackTempVo -> trackTempVo));
        List<UserListenProcessTempVo> userListenProcessTempVoList = null;
        if (!CollectionUtils.isEmpty(userListenProcessList)) {
            userListenProcessTempVoList = userListenProcessList.stream().map(item -> {
                AlbumTempVo albumTempVo = albumTempVoMap.get(item.getAlbumId());
                TrackTempVo trackTempVo = trackTempVoMap.get(item.getTrackId());
                UserListenProcessTempVo userListenProcessTempVo = new UserListenProcessTempVo();
                userListenProcessTempVo.setId(item.getId());
                userListenProcessTempVo.setAlbumId(item.getAlbumId());
                userListenProcessTempVo.setTrackId(item.getTrackId());
                userListenProcessTempVo.setBreakSecond(item.getBreakSecond());

                String coverUrl = StringUtils.isEmpty(trackTempVo.getCoverUrl()) ? albumTempVo.getCoverUrl() : trackTempVo.getCoverUrl();
                userListenProcessTempVo.setCoverUrl(coverUrl);

                userListenProcessTempVo.setAlbumTitle(albumTempVo.getAlbumTitle());
                userListenProcessTempVo.setTrackTitle(trackTempVo.getTrackTitle());
                userListenProcessTempVo.setMediaDuration(trackTempVo.getMediaDuration());

                String playRate = userListenProcessTempVo.getBreakSecond().divide(userListenProcessTempVo.getMediaDuration()
                        , 2, RoundingMode.HALF_UP).multiply(new BigDecimal(100)) + "%";
                userListenProcessTempVo.setPlayRate(playRate);
                return userListenProcessTempVo;
            }).collect(Collectors.toList());
        }

        return new Page(pageNum, pageSize, total).setRecords(userListenProcessTempVoList);
    }
}
