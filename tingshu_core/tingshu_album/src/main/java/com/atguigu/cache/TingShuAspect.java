package com.atguigu.cache;

import com.atguigu.constant.RedisConstant;
import com.atguigu.entity.AlbumInfo;
import com.atguigu.util.SleepUtils;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Aspect
public class TingShuAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RBloomFilter albumBloomFilter;
    ThreadLocal<String> threadLocal = new ThreadLocal<>();

    //5.切面编程+双重检查+本地锁=牛逼大了
    @SneakyThrows
    @Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint) {
        //a.获取目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //b.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //c.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);
        //d.拿到注解上面的值
        String prefix = tingShuCache.value();

        Object firstParam = methodParams[0];
        String cacheKey = prefix + ":" + firstParam;
        String lockKey = "lock-" + firstParam;
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        //判断是否需要加锁--性能问题
        if (redisObject == null) {
            synchronized (lockKey.intern()) {
                Object objectDb = joinPoint.proceed();
                redisTemplate.opsForValue().set(cacheKey, objectDb);
                return objectDb;
            }
        }
        return redisObject;
    }

    public static void main(String[] args) {
        String a = new String("43");
        String b = new String("43");
//        System.out.println(a==b);
//        System.out.println(a.equals(b));
        System.out.println(a.intern()==b.intern());

        String c="lock="+"24";
        String d="lock="+"24";
        System.out.println(c==d);
        System.out.println(c.equals(d));
    }


    //4.切面编程+读写锁=牛逼plus
    @SneakyThrows
    //@Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice4(ProceedingJoinPoint joinPoint) {
        //a.获取目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //b.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //c.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);
        //d.拿到注解上面的值
        String prefix = tingShuCache.value();

        Object firstParam = methodParams[0];
        String cacheKey = prefix + ":" + firstParam;
        String lockKey = "lock-" + firstParam;
        RReadWriteLock rwLock = redissonClient.getReadWriteLock(lockKey);
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        //判断是否需要加锁--性能问题
        try {
            if (redisObject == null) {
                rwLock.readLock().lock();
                redisObject = redisTemplate.opsForValue().get(cacheKey);
                rwLock.readLock().unlock();
                //判断是否是否需要从数据库中查询
                if (redisObject == null) {
                    rwLock.writeLock().lock();
                    Object objectDb = joinPoint.proceed();
                    redisTemplate.opsForValue().set(cacheKey, objectDb);
                    rwLock.writeLock().unlock();
                    return objectDb;
                }
            }
            rwLock.readLock().lock();
        } finally {
            rwLock.readLock().unlock();
        }
        return redisObject;
    }

    //3.切面编程+双重检查+分布式锁=牛逼大了
    @SneakyThrows
    //@Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice3(ProceedingJoinPoint joinPoint) {
        //a.获取目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //b.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //c.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);
        //d.拿到注解上面的值
        String prefix = tingShuCache.value();

        Object firstParam = methodParams[0];
        String cacheKey = prefix + ":" + firstParam;
        String lockKey = "lock-" + firstParam;
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        //判断是否需要加锁--性能问题
        if (redisObject == null) {
            RLock lock = redissonClient.getLock(lockKey);
            redisObject = redisTemplate.opsForValue().get(cacheKey);
            lock.lock();
            //判断是否是否需要从数据库中查询
            if (redisObject == null) {
                try {
                    Object objectDb = joinPoint.proceed();
                    redisTemplate.opsForValue().set(cacheKey, objectDb);
                    return objectDb;
                } finally {
                    lock.unlock();
                }
            }
        }
        return redisObject;
    }

    //2.切面编程+redisson+分布式锁
    @SneakyThrows
    //@Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice2(ProceedingJoinPoint joinPoint) {
        //a.获取目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //b.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //c.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);
        //d.拿到注解上面的值
        String prefix = tingShuCache.value();

        Object firstParam = null;
        String cacheKey = "";
        if (methodParams.length > 0) {
            firstParam = methodParams[0];
            cacheKey = prefix + ":" + firstParam;
        } else {
            cacheKey = prefix;
        }
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        String lockKey = "lock-" + firstParam;
        RLock lock = redissonClient.getLock(lockKey);
        if (redisObject == null) {
            try {
                lock.lock();
                //e.拿到是否需要布隆过滤器的标记
                boolean enableBloom = tingShuCache.enableBloom();
                Object objectDb = null;
                if (enableBloom) {
                    boolean flag = albumBloomFilter.contains(firstParam);
                    if (flag) {
                        objectDb = joinPoint.proceed();
                    }
                } else {
                    objectDb = joinPoint.proceed();
                }
                redisTemplate.opsForValue().set(cacheKey, objectDb);
                return objectDb;
            } finally {
                lock.unlock();
            }
        }
        return redisObject;
    }

    //1.切面编程+redis+threadlock+分布式锁
    @SneakyThrows
    //@Around("@annotation(com.atguigu.cache.TingShuCache)")
    public Object cacheAroundAdvice1(ProceedingJoinPoint joinPoint) {
        //a.获取目标方法上面的参数
        Object[] methodParams = joinPoint.getArgs();
        //b.拿到目标方法
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        //c.拿到目标方法上面的注解
        TingShuCache tingShuCache = targetMethod.getAnnotation(TingShuCache.class);
        //d.拿到注解上面的值
        String prefix = tingShuCache.value();

        Object firstParam = methodParams[0];
        String cacheKey = prefix + ":" + firstParam;
        Object redisObject = redisTemplate.opsForValue().get(cacheKey);
        //锁的粒度太大
        String lockKey = "lock-" + firstParam;
        if (redisObject == null) {
            String token = threadLocal.get();
            boolean accquireLock = false;
            if (!StringUtils.isEmpty(token)) {
                //已经拿到过锁了
                accquireLock = true;
            } else {
                //还有很多代码需要执行 1000行的  调用其他微服务 查询其他业务
                token = UUID.randomUUID().toString();
                //利用redis的setnx命令
                accquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
            }
            if (accquireLock) {
                Object objectDb = joinPoint.proceed();
                redisTemplate.opsForValue().set(cacheKey, objectDb);
                String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                //设置脚本信息
                redisScript.setScriptText(luaScript);
                //设置脚本执行后返回值
                redisScript.setResultType(Long.class);
                redisTemplate.execute(redisScript, Arrays.asList(lockKey), token);
                //擦屁股
                threadLocal.remove();
                return objectDb;
            } else {
                //自旋 目的是为了拿锁
                while (true) {
                    SleepUtils.millis(50);
                    boolean retryAccquireLock = redisTemplate.opsForValue().setIfAbsent(lockKey, token, 3, TimeUnit.SECONDS);
                    if (retryAccquireLock) {
                        threadLocal.set(token);
                        break;
                    }
                }
                return cacheAroundAdvice(joinPoint);
            }
        }
        return redisObject;

    }
}
