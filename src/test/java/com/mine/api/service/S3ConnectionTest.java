package com.mine.api.service;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
public class S3ConnectionTest {

    @Autowired
    private S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    @Test
    public void testS3Upload() {
        String testContent = "This is a test upload to verify S3 connection.";
        String key = "connection-test.txt";

        System.out.println("Attempting to upload to bucket: " + bucketName);

        try {
            s3Template.upload(bucketName, key, new ByteArrayInputStream(testContent.getBytes(StandardCharsets.UTF_8)));
            System.out.println("Upload successful!");
        } catch (Exception e) {
            System.err.println("Upload failed!");
            e.printStackTrace();
            throw e;
        }
    }
}
