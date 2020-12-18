package co.mega.vs.bean.impl;

import co.mega.vs.bean.IHttpTool;
import co.mega.vs.bean.IImageService;
import co.mega.vs.config.IConfigService;
import co.mega.vs.entity.ImageInfo;
import co.mega.vs.entity.ImageStrategyResponse;
import co.mega.vs.utils.Constants;
import co.mega.vs.utils.UrlUtils;
import co.mega.vs.utils.WriteImageTask;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service("imageService")
public class ImageService implements IImageService {

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private IHttpTool httpTool;

    @Autowired
    private ExecutorService customExecutorService;

    @Autowired
    private UrlUtils urlUtils;

    @Autowired
    private IConfigService configService;

    private Gson gson = new Gson();

    private String imageDelePath;

    private String imageStorPath;

    private static LinkedBlockingQueue<ImageInfo> imgStorQueue = new LinkedBlockingQueue<>();

    private static LinkedBlockingQueue<String> imageDownloadQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {

        imageDelePath = configService.getConfig().get(Constants.CONFIG_IMAGE_DELETE_PATH, String.class);
        imageStorPath = configService.getConfig().get(Constants.CONFIG_IMAGE_STOR_PATH, String.class);

        new Thread( () -> {
            while (true) {
                try {
                    ImageInfo imageInfo = imgStorQueue.poll(3, TimeUnit.SECONDS);
                    if (imageInfo != null) {
                        customExecutorService.execute(new WriteImageTask(imageStorPath, imageInfo, imageDownloadQueue));
                    }
                } catch (InterruptedException e) {
                    logger.info("Exception happen when write image to disk {}", e);
                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }, "witeImageDisk").start();

    }

    @Scheduled(cron="0 0 1,5 * * * ")
    private void cleanDeleFile() {
        try {
            logger.info("start to clean image in {}", imageDelePath);
            long start = System.currentTimeMillis();
            FileUtils.cleanDirectory(new File(imageDelePath));
            long end = System.currentTimeMillis();
            logger.info("clean image in {} finished", imageDelePath);
            logger.warn("clean {} directory cost time : {} ", imageDelePath, end - start);
        } catch (IOException e) {
            logger.error("Exception happen in post process thread.", e);
        }
    }

    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) {

        logger.info("Image uploaded with size {}", imageData.length);

        imgStorQueue.offer(new ImageInfo(vehicleId, timeStamp, imageData));

        Map<String, Object> r = new HashMap<>();
        r.put("fileSize", imageData.length);
        r.put("success", true);
        return r;
    }

    @Override
    public Map<String, Object> downloadImage() {

        Map<String, Object> r = new HashMap<>();

        try {
            String imageName = imageDownloadQueue.poll(1, TimeUnit.SECONDS);
            if (imageName != null) {
                File file = new File(imageStorPath + imageName.split("_")[0] + File.separator + imageName);
                byte[] bytes = FileUtils.readFileToByteArray(file);

                Path sourcePath = Paths.get(imageStorPath + imageName.split("_")[0] + File.separator + imageName);
                Path targetPath = Paths.get(imageDelePath + imageName);
                try {
                    Files.move(sourcePath, targetPath);
                    logger.info("move image {} to  {}", imageName, imageDelePath);
                } catch (IOException e) {
                    logger.error("Exception happen when move image {}.", e);
                }

                r.put("bytes", bytes);
                r.put("fileName", file.getName());
            }
        } catch (Exception e) {
            logger.error("Exception happen when download image", e);
        } finally {
            return r;
        }
    }

    @Override
    public Map<String, Object> imageStrategy(String iccId) {

        Map<String, Object> result = new HashMap<>();
        result.put("frequency", 60);
        result.put("dailyLimit", 120);
        result.put("displacement", 20);
        result.put("retryInterval", 15);
        result.put("retryLimit", 10);
        logger.info("init image upload strategy is not directed and the number of images uploaded per day is 120");

        try {
            String imageStrategyUrl = urlUtils.getImageStrategyUrl();
            Map<String, String> params = new HashMap<>();
            params.put("iccid", iccId);

            String rtJson = doRequestRaw(imageStrategyUrl, params);
            if (StringUtils.isNotBlank(rtJson)) {
                getImageStrategy(rtJson, result);
            } else {
                logger.warn("image strategy search failed! try again...");
                rtJson = doRequestRaw(imageStrategyUrl, params);
                if (StringUtils.isNotBlank(rtJson)) {
                    getImageStrategy(rtJson, result);
                } else {
                    logger.warn("image strategy search still failed in second try");
                }
            }
        } catch (Exception e) {
            logger.error("Exception happens when get image strategy : ", e);
        } finally {
            return result;
        }
    }

    private void getImageStrategy(String rtJson, Map<String, Object> result) {
        ImageStrategyResponse imageStrategyResponse = gson.fromJson(rtJson, ImageStrategyResponse.class);
        if (imageStrategyResponse.resultCode != null && imageStrategyResponse.resultCode.equals("success") && imageStrategyResponse.data != null) {
            if (Boolean.TRUE.equals(imageStrategyResponse.data.isDirected)) {
                result.put("dailyLimit", 720);
                logger.info("change image upload strategy to directed and the number of images uploaded per day is 720");
            }
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
