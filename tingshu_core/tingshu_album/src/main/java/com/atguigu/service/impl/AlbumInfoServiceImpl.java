package com.atguigu.service.impl;

import com.atguigu.SearchFeignClient;
import com.atguigu.cache.TingShuCache;
import com.atguigu.constant.KafkaConstant;
import com.atguigu.constant.RedisConstant;
import com.atguigu.constant.SystemConstant;
import com.atguigu.entity.AlbumAttributeValue;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.entity.AlbumStat;
import com.atguigu.mapper.AlbumInfoMapper;
import com.atguigu.service.AlbumAttributeValueService;
import com.atguigu.service.AlbumInfoService;
import com.atguigu.service.AlbumStatService;
import com.atguigu.service.KafkaService;
import com.atguigu.util.AuthContextHolder;
import com.atguigu.util.MongoUtil;
import com.atguigu.util.SleepUtils;
import com.atguigu.vo.AlbumTempVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 专辑信息 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Service
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {
    @Autowired
    private AlbumAttributeValueService albumAttributeValueService;
    @Autowired
    private AlbumStatService albumStatService;
    @Autowired
    private SearchFeignClient searchFeignClient;
    @Autowired
    private KafkaService kafkaService;

    //面试题 什么是事务
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
                //设置专辑id 是否能拿到id？
                albumAttributeValue.setAlbumId(albumInfo.getId());
                //albumAttributeValueService.save(albumAttributeValue);
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //保存专辑的统计信息
        List<AlbumStat> albumStatList = buildAlbumStatData(albumInfo.getId());
        albumStatService.saveBatch(albumStatList);
        //TODO 后面再说
        //如果公开专辑 把专辑信息添加到ES中
        if (SystemConstant.OPEN_ALBUM.equals(albumInfo.getIsOpen())) {
            //searchFeignClient.onSaleAlbum(albumInfo.getId());
            kafkaService.sendMessage(KafkaConstant.ONSALE_ALBUM_QUEUE, String.valueOf(albumInfo.getId()));
        }
        //把新增专辑放到布隆过滤器里面
//      RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConstant.ALBUM_BLOOM_FILTER);
//      bloomFilter.add(albumInfo.getId());
    }

    @TingShuCache("albumInfo")
    @Override
    public AlbumInfo getAlbumInfoById(Long albumId) {
        AlbumInfo albumInfo = getAlbumInfoFromDB(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedis(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedisWitThreadLocal(albumId);
        //AlbumInfo albumInfo = getAlbumInfoFromRedisson(albumId);
        return albumInfo;
    }

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter bloomFilter;

    private AlbumInfo getAlbumInfoFromRedisson(Long albumId) {
        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        String lockKey = "lock-" + albumId;
        RLock lock = redissonClient.getLock(lockKey);
        if (albumInfoRedis == null) {
            lock.lock();
            try {
                boolean flag = bloomFilter.contains(albumId);
                if (flag) {
                    AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
                    redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
                    return albumInfoDB;
                }
            } finally {
                lock.unlock();
            }
        }
        return albumInfoRedis;
    }


    ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private AlbumInfo getAlbumInfoFromRedisWitThreadLocal(Long albumId) {
        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        //锁的粒度太大
        String lockKey = "lock-" + albumId;
        if (albumInfoRedis == null) {
            String token = threadLocal.get();
            boolean accquireLock = false;
            if (!StringUtils.isEmpty(token)) {
                accquireLock = true;
            } else {
                token = UUID.randomUUID().toString();
                accquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
            }
            if (accquireLock) {
                AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
                redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                redisScript.setScriptText(luaScript);
                redisScript.setResultType(Long.class);
                redisTemplate.execute(redisScript, Arrays.asList(lockKey), token);
                //擦屁股
                threadLocal.remove();
                return albumInfoDB;
            } else {
                while (true) {
                    SleepUtils.millis(50);
                    boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
                    if (retryAccquireLock) {
                        threadLocal.set(token);
                        break;
                    }
                }
                return getAlbumInfoFromRedisWitThreadLocal(albumId);
            }
        }
        return albumInfoRedis;


    }

    @Autowired
    private RedisTemplate redisTemplate;

    private AlbumInfo getAlbumInfoFromRedis(Long albumId) {
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        GenericJackson2JsonRedisSerializer jackson2JsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
//        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        String cacheKey = RedisConstant.ALBUM_INFO_PREFIX + albumId;
        AlbumInfo albumInfoRedis = (AlbumInfo) redisTemplate.opsForValue().get(cacheKey);
        if (albumInfoRedis == null) {
            AlbumInfo albumInfoDB = getAlbumInfoFromDB(albumId);
            redisTemplate.opsForValue().set(cacheKey, albumInfoDB);
            return albumInfoDB;
        }
        return albumInfoRedis;
    }

    @NotNull
    private AlbumInfo getAlbumInfoFromDB(Long albumId) {
        AlbumInfo albumInfo = getById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueService.list(wrapper);
        albumInfo.setAlbumPropertyValueList(albumAttributeValueList);
        return albumInfo;
    }

    @Override
    public void updateAlbumInfo(AlbumInfo albumInfo) {
        //修改专辑基本信息
        updateById(albumInfo);
        //删除原有专辑标签属性信息
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumInfo.getId());
        albumAttributeValueService.remove(wrapper);
        //保存专辑标签属性
        List<AlbumAttributeValue> albumPropertyValueList = albumInfo.getAlbumPropertyValueList();
        if (!CollectionUtils.isEmpty(albumPropertyValueList)) {
            for (AlbumAttributeValue albumAttributeValue : albumPropertyValueList) {
                //设置专辑id
                albumAttributeValue.setAlbumId(albumInfo.getId());
            }
            albumAttributeValueService.saveBatch(albumPropertyValueList);
        }
        //TODO 还有其他事情要做 修改缓存里面的信息-作业
        //如果公开专辑 把专辑信息添加到ES中
        if (SystemConstant.OPEN_ALBUM.equals(albumInfo.getIsOpen())) {
            //searchFeignClient.onSaleAlbum(albumInfo.getId());
            kafkaService.sendMessage(KafkaConstant.ONSALE_ALBUM_QUEUE, String.valueOf(albumInfo.getId()));
        } else {
            //searchFeignClient.offSaleAlbum(albumInfo.getId());
            kafkaService.sendMessage(KafkaConstant.OFFSALE_ALBUM_QUEUE, String.valueOf(albumInfo.getId()));
        }
    }

    @Override
    public void deleteAlbumInfo(Long albumId) {
        removeById(albumId);
        LambdaQueryWrapper<AlbumAttributeValue> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AlbumAttributeValue::getAlbumId, albumId);
        albumAttributeValueService.remove(wrapper);
        albumStatService.remove(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, albumId));
        //TODO 还有其他事情要做
        //searchFeignClient.offSaleAlbum(albumId);
        kafkaService.sendMessage(KafkaConstant.OFFSALE_ALBUM_QUEUE, String.valueOf(albumId));
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public boolean isSubscribe(Long albumId) {
        //db.createCollection("userSubscribe_14")
        //db.userSubscribe_14.insertOne({userId:'14',albumId:'139'})
        Long userId = AuthContextHolder.getUserId();
        Query query = Query.query(Criteria.where("userId").is(userId).and("albumId").is(albumId));
        long count = mongoTemplate.count(query, MongoUtil.getCollectionName(MongoUtil.MongoCollectionEnum.USER_SUBSCRIBE, userId));
        if (count > 0) return true;
        return false;
    }

    @Override
    public List<AlbumTempVo> getAlbumTempList(List<Long> albumIdList) {
        List<AlbumInfo> albumInfoList = listByIds(albumIdList);
        return albumInfoList.stream().map(albumInfo -> {
            AlbumTempVo albumTempVo = new AlbumTempVo();
            BeanUtils.copyProperties(albumInfo, albumTempVo);
            albumTempVo.setAlbumId(albumInfo.getId());
            return albumTempVo;
        }).collect(Collectors.toList());
    }

    private List<AlbumStat> buildAlbumStatData(Long albumId) {
        ArrayList<AlbumStat> albumStatList = new ArrayList<>();
        initAlbumStat(albumId, albumStatList, SystemConstant.PLAY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.SUBSCRIBE_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.BUY_NUM_ALBUM);
        initAlbumStat(albumId, albumStatList, SystemConstant.COMMENT_NUM_ALBUM);
        return albumStatList;
    }

    private static void initAlbumStat(Long albumId, ArrayList<AlbumStat> albumStatList, String statType) {
        AlbumStat albumStat = new AlbumStat();
        albumStat.setAlbumId(albumId);
        albumStat.setStatType(statType);
        albumStat.setStatNum(0);
        albumStatList.add(albumStat);
    }
}
