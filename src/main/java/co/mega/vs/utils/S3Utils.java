package co.mega.vs.utils;

import co.mega.vs.dao.imp.CamStatusDao;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class S3Utils {
    private static final Gson gson = new Gson();

    private static Logger logger = LoggerFactory.getLogger(S3Utils.class);

    public static AmazonS3 getS3Client(S3Type s3Type) {
        AmazonS3 s3Client = null;
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setSignerOverride("AWSS3V4SignerType");
        AWSCredentials credentials;
        switch (s3Type) {
            case AWS:
                credentials = new BasicAWSCredentials(Constants.AWS_S3_ACCESS_KEY, Constants.AWS_S3_SECRET_KEY);
                s3Client = AmazonS3ClientBuilder
                        .standard()
                        .withRegion(Regions.CN_NORTHWEST_1)
                        .withPathStyleAccessEnabled(true)
                        .withClientConfiguration(clientConfiguration)
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .build();
                logger.info("aws s3 initialized...");
                break;
            case HW:
                credentials = new BasicAWSCredentials(Constants.HW_OBS_ACCESS_KEY, Constants.HW_OBS_SECRET_KEY);
                s3Client = AmazonS3ClientBuilder
                        .standard()
                        .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Constants.HW_OBS_ENDPOINT_URL, "cn-south-1"))
                        .withPathStyleAccessEnabled(true)
                        .withClientConfiguration(clientConfiguration)
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .build();
                logger.info("hw s3 initialized...");
                break;
        }

        return s3Client;
    }

    public static String getBucketByFileName(boolean isImage, String vehicleId, S3Type s3Type) {
        if(StringUtils.isNullOrEmpty(vehicleId) || s3Type == null) {
            return null;
        }

        String prefix = s3Type.equals(S3Type.AWS) ? Constants.AWS_BUCKET_NAME_PREFIX : Constants.GN_BUCKET_NAME_PREFIX;

        Date date=new Date();
        SimpleDateFormat dateFormat=new SimpleDateFormat("YYYYMMdd");
        String dateStr = dateFormat.format(date);

        if (isImage) {
            return prefix + "/vision-server/image/" + dateStr + "/" + vehicleId;
        } else {
            return prefix + "/vision-server/log/" + dateStr + "/" + vehicleId;
        }

    }

    public static void main(String[] args) {
        AmazonS3 amazonS3 = getS3Client(S3Type.HW);
        amazonS3.getObject(new GetObjectRequest("ais-storage-gz/audio_data/2019-09-30", "01ad6500-616d-46d2-a580-a3b4fd0f6276"), new File("/tmp/01ad6500-616d-46d2-a580-a3b4fd0f6276"));
        System.out.println(amazonS3.getRegion());
    }
}
