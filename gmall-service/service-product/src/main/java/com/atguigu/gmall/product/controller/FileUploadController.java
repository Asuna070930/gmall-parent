package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.FutureTask;

@RestController
@RequestMapping("/admin/product")
public class FileUploadController {


    @Value("${minio.endpointUrl}")
    private String endpointUrl;

    @Value("${minio.accessKey}")
    private String accessKey;

    @Value("${minio.secreKey}")
    private String secreKey;

    @Value("${minio.bucketName}")
    private String bucketName;


    /**
     * 文件上传
     * /admin/product/fileUpload
     * @return
     */
    @PostMapping("/fileUpload")
    public Result fileUpload(MultipartFile file){
        //定义文件上传路径
        String fileUrl;
        try {
            // 创建操作minio服务的客户端，并且设置用户名和密码
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secreKey)
                            .build();

            // 判断桶是否存在，不存在创建
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // 不存在创建指定名称的桶
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket 'asiatrip' already exists.");
            }

            //制定上传文件的名称
            String filename=System.currentTimeMillis()+ UUID.randomUUID().toString().replaceAll("-","").substring(0,3)+file.getOriginalFilename();


            //上传文件
            // Upload known sized input stream.
            //上传知道内容大小的输入流
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(filename).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            //编辑文件访问路径，用于选择图片上传后页面的回显

            //图片上传的路径组成分析： http://192.168.254.150:9000/gmall/img/4.png
            //  endpointUrl+/+bucketName+/filename
             fileUrl=endpointUrl+"/"+bucketName+"/"+filename;


        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return Result.ok(fileUrl);
    }
}
