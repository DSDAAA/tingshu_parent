package com.atguigu.minio;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class FileUploader {
    public static void main(String[] args) throws Exception {
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient = MinioClient.builder()
                            .endpoint("http://192.168.76.100:9000")
                            .credentials("enjoy6288", "enjoy6288")
                            .build();
            //创建一个桶
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket("tingshu").build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket("tingshu").build());
            } else {
                System.out.println("桶'tingshu'已经存在");
            }
            //上传文件
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("tingshu")
                            .object("baby.jpg")
                            .filename("C:\\Users\\Administrator\\Desktop\\230710\\images\\22.jpg")
                            .build());
            System.out.println("文件上传成功");
        } catch (MinioException e) {
            System.out.println("Error occurred: " + e);
            System.out.println("HTTP trace: " + e.httpTrace());
        }
    }
}
