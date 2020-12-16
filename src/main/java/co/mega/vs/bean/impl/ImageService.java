package co.mega.vs.bean.impl;

import co.mega.vs.bean.IHttpTool;
import co.mega.vs.bean.IImageService;
import co.mega.vs.config.IConfigService;
import co.mega.vs.entity.ImageStatus;
import co.mega.vs.entity.ImageStrategyResponse;
import co.mega.vs.utils.Constants;
import co.mega.vs.utils.UrlUtils;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service("imageService")
public class ImageService implements IImageService {

//    private static final String PATH = "/Users/h0153/Desktop/";

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private IHttpTool httpTool;

    @Autowired
    private UrlUtils urlUtils;

    @Autowired
    private IConfigService configService;

    private Gson gson = new Gson();

    private Map<String, Object> lockMap = new ConcurrentHashMap<>();

    private LinkedBlockingQueue<ImageStatus> imgStatusQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {

        //init imageStatusQueue
        File imageDeleteFile = new File(configService.getConfig().get(Constants.IMAGE_DELETE_PATH, String.class));
        File imageDirFile = new File(configService.getConfig().get(Constants.IMAGE_STOR_PATH, String.class));
        Arrays.asList(imageDirFile.listFiles()).forEach(vidFile -> {
            Arrays.asList(vidFile.listFiles()).forEach(imageFile -> imgStatusQueue.offer(new ImageStatus(imageFile.getName(), new AtomicInteger(0))));
        });

        Calendar cal = Calendar.getInstance();
        new Thread(()->{
            while (true) {
                if (imgStatusQueue.isEmpty()) {
                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    int hour = cal.get(Calendar.HOUR_OF_DAY); //美东？
                    if (0 < hour && hour < 6) {
                        try {
                            FileUtils.cleanDirectory(imageDeleteFile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    ImageStatus tmpImageStatus = imgStatusQueue.peek();
                    while (tmpImageStatus != null && tmpImageStatus.getStatus().intValue() == 1) {
                        Path sourcePath = Paths.get(configService.getConfig().get(Constants.IMAGE_STOR_PATH, String.class) + tmpImageStatus.getFileName().split("_")[0] + File.separator + tmpImageStatus.getFileName());
                        Path targetPath = Paths.get(configService.getConfig().get(Constants.IMAGE_DELETE_PATH, String.class) + tmpImageStatus.getFileName());
                        try {
                            Files.move(sourcePath, targetPath);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        imgStatusQueue.poll();
                        tmpImageStatus = imgStatusQueue.peek();
                    }

                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) throws IOException {

        logger.info("Image uploaded with size {}", imageData.length);

        try {
            FileUtils.writeByteArrayToFile(new File(configService.getConfig().get(Constants.IMAGE_STOR_PATH, String.class) + vehicleId + File.separator + vehicleId + "_" + timeStamp), imageData);
            ImageStatus imageStatus = new ImageStatus(vehicleId + "_" + timeStamp, new AtomicInteger(0));
            imgStatusQueue.offer(imageStatus);
        } catch (IOException e) {
            throw e;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("fileSize", imageData.length);
        r.put("success", true);
        return r;
    }

    @Override
    public Map<String, Object> downloadImage() throws IOException {
        Map<String, Object> r = new HashMap<>();

        for (ImageStatus imageStatus : imgStatusQueue) {
            if (imageStatus.getStatus().get() == 0) {
                synchronized (imageStatus) {
                    if (imageStatus.getStatus().get() == 0) {
                        if (StringUtils.isNotBlank(imageStatus.getFileName())) {
                            String vidDir = imageStatus.getFileName().split("_")[0];
                            File file = new File(configService.getConfig().get(Constants.IMAGE_STOR_PATH, String.class) + vidDir + File.separator + imageStatus.getFileName());
                            byte[] bytes = FileUtils.readFileToByteArray(file);
                            r.put("bytes", bytes);
                            r.put("fileName", file.getName());
                        }
                        imageStatus.setStatus(new AtomicInteger(1));
                        return r;
                    }
                }
            }
        }
        return r;
    }

    @Override
    public Map<String, Object> imageStrategy(String iccId) {

        Map<String, Object> result = new HashMap<>();

        String imageStrategyUrl = urlUtils.getImageStrategyUrl();
        Map<String, String> params = new HashMap<>();
        params.put("iccid", iccId);

        String rtJson = doRequestRaw(imageStrategyUrl, params);
        if (StringUtils.isNotBlank(rtJson)) {
            return getImageStrategy(rtJson);
        } else {
            logger.warn("image strategy search failed! try again...");
            rtJson = doRequestRaw(imageStrategyUrl, params);
            if (StringUtils.isBlank(rtJson)) {
                logger.warn("image strategy search still failed in second try");
                return result;
            } else {
                return getImageStrategy(rtJson);
            }
        }
    }

    private Map<String, Object> getImageStrategy(String rtJson) {

        Map<String, Object> result = new HashMap<>();
        result.put("frequency", 60);
        result.put("dailyLimit", 120);
        result.put("displacement", 20);
        result.put("retryInterval", 15);
        result.put("retryLimit", 10);

        try {
            ImageStrategyResponse imageStrategyResponse = gson.fromJson(rtJson, ImageStrategyResponse.class);
            if (imageStrategyResponse.resultCode != null && imageStrategyResponse.resultCode.equals("success") && imageStrategyResponse.data != null) {
                if (Boolean.TRUE.equals(imageStrategyResponse.data.isDirected)) {
                    result.put("dailyLimit", 720);
                    logger.info("image upload strategy is directed and and the number of images uploaded per day is 720");
                }
            }
        } catch (Exception e) {
            logger.warn("Error when parsing image strategy response: ", e);
        } finally {
            return result;
        }
    }

    private String doRequestRaw(String url, Map<String, String> params) {
        String finalUrl = null;
        try {
            URIBuilder b = new URIBuilder(url);
            params.entrySet().forEach(entry -> b.addParameter(entry.getKey(), entry.getValue()));
            finalUrl = b.build().toString();
        } catch (URISyntaxException e) {
            logger.error("URI syntax error: ", e);
        }

        if (finalUrl != null) {
            String rtJson = httpTool.queryGet(new QueryUrl(finalUrl, QueryUrl.QueryType.OTHER), true, null);
            return rtJson;
        }

        return "";
    }
}
