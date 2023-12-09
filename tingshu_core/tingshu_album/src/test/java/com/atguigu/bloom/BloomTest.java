package com.atguigu.bloom;

import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BloomTest {
    @Autowired
    private RBloomFilter bloomFilter;
    @Test
    public void albumBloomTest(){
        boolean flag1500 = bloomFilter.contains(1500L);
        System.out.println(flag1500);
        boolean flag1800 = bloomFilter.contains(1800L);
        System.out.println(flag1800);
    }
}
