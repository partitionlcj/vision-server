package co.mega.vs.bean.impl;

import co.mega.vs.bean.IS3Uploader;
import co.mega.vs.config.IConfigService;
import co.mega.vs.entity.LogFileInfo;
import co.mega.vs.utils.Constants;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Component
public class S3Uploader extends Thread implements IS3Uploader {

    public static final Logger logger = LoggerFactory.getLogger(S3Uploader.class);
    private static AmazonS3 s3Client = null;

    private int queueSize;

    private static String bucketNamePrefix;

    private BlockingDeque<LogFileInfo> logFileQueue;

    @Autowired
    private IConfigService configService;

    @PostConstruct
    public void init() {
        queueSize = configService.getConfig().get(Constants.CONFIG_LOD_FILE_QUEUE_SIZE, Integer.class);
        logFileQueue = new LinkedBlockingDeque<>(queueSize);

        String env = configService.getConfig().get(Constants.CONFIG_ENV, String.class);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");

        if (env.startsWith("vs-gn-")) {
            bucketNamePrefix = Constants.GN_BUCKET_NAME_PREFIX;
            AWSCredentials obsCredentials = new BasicAWSCredentials(Constants.HW_OBS_ACCESS_KEY, Constants.HW_OBS_SECRET_KEY);
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Constants.HW_OBS_ENDPOINT_URL, ""))
                    .withPathStyleAccessEnabled(true)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(obsCredentials))
                    .build();
            logger.info("obs client initialized...");
        } else {
            bucketNamePrefix = Constants.AWS_BUCKET_NAME_PREFIX;
            AWSCredentials awsCredentials = new BasicAWSCredentials(Constants.AWS_S3_ACCESS_KEY, Constants.AWS_S3_SECRET_KEY);
            s3Client = AmazonS3ClientBuilder
                    .standard()
                    .withRegion(Regions.CN_NORTHWEST_1)
                    .withPathStyleAccessEnabled(true)
                    .withClientConfiguration(clientConfiguration)
                    .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                    .build();
            logger.info("s3 client initialized...");
        }

        this.start();

    }

    @Override
    public void run() {
        while (true) {
            try {
                LogFileInfo logFileInfo = logFileQueue.take();
                String bucketByFileName = getBucketByFileName(logFileInfo.isImage(), logFileInfo.getVehicleId());

                uploadToS3(logFileInfo.getFile(), bucketByFileName, logFileInfo.getRequestId());
            } catch (Exception e) {
                logger.error("log file upload to s3 error: ", e);
            }
        }
    }

    @Override
    public void add(LogFileInfo logFileInfo) {
        try {
            logFileQueue.offer(logFileInfo);
        } catch (Exception e) {
            logger.error("log file add to queue before upload to s3 failed", e);
        }
    }

    private void uploadToS3(byte[] fileBytes, String bucketName, String fileName) {

        try {
            // 上传文件
            logger.info("Uploading {} to bucket {}", fileName, bucketName);
            if (bucketName != null) {
                InputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(fileBytes.length);
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

    private String getBucketByFileName(boolean isImage, String vehicleId) {
        if(StringUtils.isNullOrEmpty(vehicleId)) {
            return null;
        }

        Date date=new Date();
        SimpleDateFormat dateFormat=new SimpleDateFormat("YYYYMMdd");
        String dateStr = dateFormat.format(date);

        if (isImage) {
            return bucketNamePrefix + "/vision-server/image/" + dateStr + "/" + vehicleId;
        } else {
            return bucketNamePrefix + "/vision-server/log/" + dateStr + "/" + vehicleId;
        }

    }
}
