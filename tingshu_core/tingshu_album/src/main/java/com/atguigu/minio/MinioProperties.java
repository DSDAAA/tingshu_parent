package com.atguigu.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endPoint;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
