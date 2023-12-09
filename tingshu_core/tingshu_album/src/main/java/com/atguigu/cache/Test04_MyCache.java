package com.atguigu.cache;

import com.atguigu.util.SleepUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Test04_MyCache {
    private Map cache = new HashMap<String, Object>();
    //读写锁
    private ReentrantReadWriteLock rwLock=new ReentrantReadWriteLock();
    public Object getData(String key) {
        Object value;
        try {
            rwLock.readLock().lock();
            value = cache.get(key);
            rwLock.readLock().unlock();
            if (value == null) {
                rwLock.writeLock().lock();
                //模拟从数据库中查询
                value = "you are so intelligent";
                cache.put(key, value);
                rwLock.writeLock().unlock();
            }
            rwLock.readLock().lock();
        } finally {
            rwLock.readLock().unlock();
        }
        return value;
    }

//    public Object getData(String key) {
//        Object value = cache.get(key);
//        if (value == null) {
//            //模拟从数据库中查询
//            value = "you are so intelligent";
//            cache.put(key, value);
//        }
//        return value;
//    }

    public static void main(String[] args) {
        Test04_MyCache myCache = new Test04_MyCache();
        Object retVal1 = myCache.getData("atguigu");
        System.out.println(retVal1);
        Object retVal2 = myCache.getData("atguigu");
        System.out.println(retVal2);
    }
}
