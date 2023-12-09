package com.atguigu.service.impl;

import com.atguigu.entity.BaseCategory1;
import com.atguigu.mapper.BaseCategory1Mapper;
import com.atguigu.service.BaseCategory1Service;
import com.atguigu.util.SleepUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 一级分类表 服务实现类
 * </p>
 *
 * @author Dunston
 * @since 2023-11-29
 */
@Service
public class BaseCategory1ServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategory1Service {
    @Autowired
    private RedisTemplate redisTemplate;

    //@Override
    public void setNum00() {
        String num = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(num)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int value = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num", String.valueOf(++value));
        }
    }

    //@Override
    public synchronized void setNum000() {
        String num = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(num)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int value = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num", String.valueOf(++value));
        }
    }

    private void doBusiness() {
        String num = (String) redisTemplate.opsForValue().get("num");
        if (StringUtils.isEmpty(num)) {
            redisTemplate.opsForValue().set("num", "1");
        } else {
            int value = Integer.parseInt(num);
            redisTemplate.opsForValue().set("num", String.valueOf(++value));
        }
    }

    //分布式锁案例一
    //@Override
    public void setNum01() {
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok");
        if (accquireLock) {
            //拿到锁了 出现异常或错误 导致锁无法删除 释放
            doBusiness();
            //执行完了之后需要删除锁
            redisTemplate.delete("lock");
        } else {
            //如果没有拿到锁 递归
            setNum();
        }
    }

    //分布式锁案例二
    //@Override
    public void setNum02() {
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", "ok", 3, TimeUnit.SECONDS);
        if (accquireLock) {
            Thread thread = new Thread(() -> {
                //每隔3秒 续期5秒钟
                while (true) {
                    SleepUtils.sleep(3);
                    redisTemplate.expire("lock", 5, TimeUnit.SECONDS);
                    System.out.println("续期成功");
                }
            });
            thread.setDaemon(true);
            thread.start();
            //拿到锁了
            doBusiness();

            //执行完了之后需要删除锁
            redisTemplate.delete("lock");

        } else {
            //如果没有拿到锁 递归
            setNum();
        }
    }

    //分布式锁案例三
    //@Override
    public void setNum03() {
        //放一个标记
        String token = UUID.randomUUID().toString();
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.SECONDS);
        if (accquireLock) {
            //拿到锁了
            doBusiness(); //31s
            //执行完了之后需要删除锁
            String redisToken = (String) redisTemplate.opsForValue().get("lock");
            if (token.equals(redisToken)) {
                redisTemplate.delete("lock");
            }
        } else {
            //如果没有拿到锁 递归
            setNum();
        }
    }

    //分布式锁案例四
    //@Override
    public void setNum04() {
        //放一个标记
        String token = UUID.randomUUID().toString();
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.SECONDS);
        if (accquireLock) {
            //拿到锁了
            doBusiness(); //31s
            //执行完了之后需要删除锁  KEYS[1]=lock   ARGV[1]=token
           /* String redisToken =(String) redisTemplate.opsForValue().get("lock");
            if(token.equals(redisToken)){
                redisTemplate.delete("lock");
            }*/
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //设置脚本信息
            redisScript.setScriptText(luaScript);
            //设置脚本返回内容
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);

        } else {
            //如果没有拿到锁 递归
            setNum();
        }
    }

    //分布式锁案例五
    //@Override
    public void setNum05() {
        //还有很多代码要执行 1000行 调用其他微服务 查询其他业务 查询数据库等
        //放一个标记
        String token = UUID.randomUUID().toString();
        //利用redis的setnx命令
        boolean accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        if (accquireLock) {
            //拿到锁了
            doBusiness();
            //执行完了之后需要删除锁  KEYS[1]=lock   ARGV[1]=token
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            //设置脚本信息
            redisScript.setScriptText(luaScript);
            //设置脚本返回内容
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
        } else {
            //目的是为了拿锁 自旋
            while (true) {
                SleepUtils.millis(50);
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryAccquireLock) {
                    break;
                }
            }
            setNum();
        }
    }

    //分布式锁案例六--锁不具备可重入性
    Map<Thread, Boolean> threadMap = new HashMap();

    //@Override
    public void setNum06() {
        Boolean flag = threadMap.get(Thread.currentThread());
        boolean accquireLock = false;
        String token = null;
        if (flag != null && flag) {
            accquireLock = true;
        } else {
            token = UUID.randomUUID().toString();
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        }
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
        } else {
            while (true) {
                SleepUtils.millis(50);
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryAccquireLock) {
                    threadMap.put(Thread.currentThread(), true);
                    break;
                }
            }
            setNum();
        }
    }

    /**
     * 分布式锁案例七
     * 长此以往会造成 内存泄漏
     * a.通过线上日志 抓取当前应用的内存模型
     * b.通过jvisualvm发现有个对象内存空间不断的在上涨
     */
    Map<Thread, String> threadMap1 = new HashMap();
    ThreadLocal<String> threadLocal = new ThreadLocal<>();

    //@Override
    public void setNum07() {
        String token = threadLocal.get();
        boolean accquireLock = false;
        if (!StringUtils.isEmpty(token)) {
            accquireLock = true;
        } else {
            token = UUID.randomUUID().toString();
            accquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
        }
        if (accquireLock) {
            doBusiness();
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript, Arrays.asList("lock"), token);
            //擦屁股
            threadLocal.remove();
        } else {
            while (true) {
                SleepUtils.millis(50);
                boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent("lock", token, 3, TimeUnit.HOURS);
                if (retryAccquireLock) {
                    threadLocal.set(token);
                    break;
                }
            }
            setNum();
        }
    }

    @Autowired
    private RedissonClient redissonClient;
    @Override
    public void setNum() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        doBusiness();
        lock.unlock();
    }


}
