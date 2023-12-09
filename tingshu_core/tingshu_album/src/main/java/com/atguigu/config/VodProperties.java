package com.atguigu.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="vod") //读取节点
@Data
public class VodProperties {
    private Integer appId;
    private String secretId;
    private String secretKey;
    private String region;
    private String procedure;
    private String tempPath;
    private String playKey;
}
