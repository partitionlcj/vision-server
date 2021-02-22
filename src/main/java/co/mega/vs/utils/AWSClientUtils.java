package co.mega.vs.utils;

import co.mega.vs.config.IConfigService;
import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class AWSClientUtils {

    @Autowired
    private IConfigService configService;

    private S3Type s3Type;

    private ThreadLocal<AmazonS3> threadLocal = ThreadLocal.withInitial(()-> S3Utils.getS3Client(s3Type));

    public AmazonS3 getAWSClient() {
        return threadLocal.get();
    }

    @PostConstruct
    public void init() throws Exception {
        String env = configService.getConfig().get(Constants.CONFIG_ENV, String.class);
        s3Type = env.contains("gn") ? S3Type.HW : S3Type.AWS;
    }
}
