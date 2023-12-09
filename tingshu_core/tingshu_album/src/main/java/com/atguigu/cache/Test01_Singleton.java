package com.atguigu.cache;

public class Test01_Singleton {
    private Test01_Singleton() {
    }
    private volatile static Test01_Singleton instance;
    public static Test01_Singleton getInstance() {
        if (instance == null) {
            synchronized (Test01_Singleton.class) {
                if (instance == null) {
                    //memory =allocate();    //1：分配对象的内存空间
                    //ctorInstance(memory);  //2：初始化对象
                    //instanceA =memory;     //3：设置instance指向刚分配的内存地址
                    instance = new Test01_Singleton();
                    //JVM对指令进行重排
                    //memory =allocate();    //1：分配对象的内存空间
                    //instanceA =memory;     //3：设置instance指向刚分配的内存地址
                    //ctorInstance(memory);  //2：初始化对象
                }
            }
        }
        return instance;
    }
}
