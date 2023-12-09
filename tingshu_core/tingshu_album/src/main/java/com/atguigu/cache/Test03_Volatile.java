package com.atguigu.cache;

import com.atguigu.util.SleepUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class Test03_Volatile {
    //不具备原子性
    private volatile Integer num = 0;
    //基于CAS实现线程安全
    AtomicInteger auto=new AtomicInteger(0);
    public Integer getNum() {
        return num;
    }
    public synchronized Integer incr() {
        //num=num+1 这是多个操作
        return ++num;
    }
    public static void main(String[] args) {
        Test03_Volatile dataOne = new Test03_Volatile();
        for (int i = 0; i < 1000; i++) {
            new Thread(() -> {
                dataOne.incr();
            }).start();
        }
        SleepUtils.millis(500);
        System.out.println(dataOne.getNum());
    }
}
