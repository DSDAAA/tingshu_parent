package com.atguigu.minio;

import io.minio.*;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
@EnableConfigurationProperties({MinioProperties.class})
public class MinioUploader {
    @Autowired
    private MinioProperties minioProperties;
    @Autowired
    private MinioClient minioClient;

    @SneakyThrows
    @Bean
    public MinioClient minioClient() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(minioProperties.getEndPoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioProperties.getBucketName()).build());
        if (!found) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioProperties.getBucketName()).build());
        } else {
            System.out.println("桶" + minioProperties.getBucketName() + "已经存在");
        }
        return minioClient;
    }

    public String uploadFile(MultipartFile file) throws Exception {
        //设置储存对象名称
        String prefix = UUID.randomUUID().toString().replaceAll("-", "");
        String originalFilename = file.getOriginalFilename();
        String suffix = FilenameUtils.getExtension(originalFilename);
        String fileName = prefix + "." + suffix;
        //上传文件
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(fileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
        String retUrl = minioProperties.getEndPoint() + "/" + minioProperties.getBucketName() + "/" + fileName;
        return retUrl;
    }
}
