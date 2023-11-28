package com.atguigu.util;

import lombok.Getter;

public class MongoUtil {

    @Getter
    public enum MongoCollectionEnum {

        USER_SUBSCRIBE(100,"userSubscribe"),
        USER_COLLECT(100,"userCollect"),
        USER_LISTEN_PROCESS(100,"userListenProcess"),
        COMMENT(100,"comment"),
        COMMENT_PRAISE(100,"commentPraise"),
        ;

        private Integer partition;
        private String collectionPrefix;

        MongoCollectionEnum(Integer partition, String collectionPrefix) {
            this.partition = partition;
            this.collectionPrefix = collectionPrefix;
        }

    }


    /**
     * 获取mongo表名
     * @param mongoCollection Collection前缀
     * @param route 路由
     * @return
     */
    public static String getCollectionName(MongoCollectionEnum mongoCollection, Long route) {
        return mongoCollection.getCollectionPrefix() + "_" + route % mongoCollection.getPartition();
    }
}

