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

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private IHttpTool httpTool;

    @Autowired
    private UrlUtils urlUtils;

    @Autowired
    private IConfigService configService;

    private Gson gson = new Gson();

    private LinkedBlockingQueue<ImageStatus> imgStatusQueue = new LinkedBlockingQueue<>();

    private String imageDelePath;

    private String imageStorPath;

    private Calendar cal = Calendar.getInstance();

    @PostConstruct
    public void init() {
        imageDelePath = configService.getConfig().get(Constants.CONFIG_IMAGE_DELETE_PATH, String.class);
        imageStorPath = configService.getConfig().get(Constants.CONFIG_IMAGE_STOR_PATH, String.class);

        //init imageStatusQueue
        File imageDeleteFile = new File(imageDelePath);
        File imageDirFile = new File(imageStorPath);
        Arrays.asList(imageDirFile.listFiles()).forEach(vidFile -> {
            Arrays.asList(vidFile.listFiles()).forEach(imageFile -> imgStatusQueue.offer(new ImageStatus(imageFile.getName(), new AtomicInteger(0))));
        });

        new Thread( () -> {
            while (true) {
                if (imgStatusQueue.isEmpty()) {
                    cleanDeleFile(imageDeleteFile);
                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        logger.error("Exception happen in post process thread.", e);
                    }
                } else {
                    cleanDeleFile(imageDeleteFile);
                    ImageStatus tmpImageStatus = imgStatusQueue.peek();
                    while (tmpImageStatus != null && tmpImageStatus.getStatus().intValue() == 1) {
                        Path sourcePath = Paths.get(imageStorPath + tmpImageStatus.getFileName().split("_")[0] + File.separator + tmpImageStatus.getFileName());
                        Path targetPath = Paths.get(imageDelePath + tmpImageStatus.getFileName());
                        try {
                            Files.move(sourcePath, targetPath);
                            logger.info("move image {} to  {}", tmpImageStatus.getFileName(), imageDeleteFile);
                        } catch (IOException e) {
                            logger.error("Exception happen in post process thread.", e);
                        }
                        imgStatusQueue.poll();
                        tmpImageStatus = imgStatusQueue.peek();
                    }

                    try {
                        Thread.sleep(30 * 1000);
                    } catch (InterruptedException e) {
                        logger.error("Exception happen in post process thread.", e);
                    }
                }
            }
        }).start();
    }

    private void cleanDeleFile(File imageDeleteFile) {
        int hour = cal.get(Calendar.HOUR_OF_DAY); //美东？
        if (0 < hour && hour < 6) {
            try {
                logger.info("start to clean image in {}", imageDelePath);
                FileUtils.cleanDirectory(imageDeleteFile);
                logger.info("clean image in {} finished", imageDelePath);
            } catch (IOException e) {
                logger.error("Exception happen in post process thread.", e);
            }
        }
    }

    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) throws IOException {

        logger.info("Image uploaded with size {}", imageData.length);

        try {
            FileUtils.writeByteArrayToFile(new File(imageStorPath + vehicleId + File.separator + vehicleId + "_" + timeStamp), imageData);
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

        Iterator<ImageStatus> iterator = imgStatusQueue.iterator();
        while (iterator.hasNext()) {
            ImageStatus imageStatus = iterator.next();
            if (imageStatus.getStatus().get() == 0 && imageStatus.getLock().tryLock()) {
                if (imageStatus.getStatus().get() == 0) {
                    if (StringUtils.isNotBlank(imageStatus.getFileName())) {
                        String vidDir = imageStatus.getFileName().split("_")[0];
                        File file = new File(imageStorPath + vidDir + File.separator + imageStatus.getFileName());
                        byte[] bytes = FileUtils.readFileToByteArray(file);
                        r.put("bytes", bytes);
                        r.put("fileName", file.getName());
                    }
                    imageStatus.setStatus(new AtomicInteger(1));
                    imageStatus.getLock().unlock();
                    return r;
                }
                imageStatus.getLock().unlock();
            }
        }

        return r;
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
