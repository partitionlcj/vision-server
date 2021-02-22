package co.mega.vs.utils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class S3Uploader {

    private static Logger logger = LoggerFactory.getLogger(S3Uploader.class);

    public static void main(String[] args) {

        if(args.length < 2) {
            System.out.println("Usage: S3Uploader [fileName] [dest]");
            System.exit(-1);
        }
        String path = args[0];
        String dest = args[1];

        File file = new File(path);
        if(!file.exists()) {
            System.out.println(String.format("file not exists: %s, please check!", path));
            System.exit(-1);
        }
//        upload(file, dest);
    }

    public static void upload(byte[] bytes, String vehicleId, String fileName, String env, boolean isImage, AmazonS3 s3Client)  {

        List<S3Type> s3Types = new ArrayList<>();
        if (StringUtils.isNotBlank(env) && env.contains("gn")) {
            s3Types.add(S3Type.HW);
        } else {
            s3Types.add(S3Type.AWS);
        }

        for (S3Type s3Type : s3Types) {
//            AmazonS3 s3Client = S3Utils.getS3Client(s3Type);
            try {
                // 上传文件
                String bucketName = S3Utils.getBucketByFileName(isImage, vehicleId, s3Type);
                logger.info("Uploading {} to {} with bucket {}", fileName, s3Type.getName(), bucketName);
                if (bucketName != null) {
                    InputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                    ObjectMetadata meta = new ObjectMetadata();
                    meta.setContentLength(bytes.length);
                    s3Client.putObject(bucketName, fileName, byteArrayInputStream, meta);
                    logger.info("upload {} done", fileName);
                } else {
                    logger.error("invalid file name, please check!", fileName);
                }
            } catch (AmazonServiceException ase) {
                logger.error("Caught an AmazonServiceException when upload file to s3", ase);
            } catch (AmazonClientException ace) {
                logger.error("Caught an AmazonClientException when upload file to s3", ace);
            } catch (Exception e) {
                logger.error("Exception happened when upload file to s3", e);
            }
        }
    }
}
