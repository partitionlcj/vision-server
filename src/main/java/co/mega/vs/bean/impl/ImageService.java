package co.mega.vs.bean.impl;

import co.mega.vs.bean.IHttpTool;
import co.mega.vs.bean.IImageService;
import co.mega.vs.bean.IS3Uploader;
import co.mega.vs.config.IConfigService;
import co.mega.vs.dao.ICamStatusDao;
import co.mega.vs.dao.IUploadLogDao;
import co.mega.vs.entity.ImageStrategyResponse;
import co.mega.vs.entity.LogFileInfo;
import co.mega.vs.utils.Constants;
import co.mega.vs.utils.UrlUtils;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@EnableScheduling
@Service("imageService")
public class ImageService implements IImageService {

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private IHttpTool httpTool;

    @Autowired
    private UrlUtils urlUtils;

    @Autowired
    private IConfigService configService;

    @Autowired
    private IS3Uploader s3Uploader;

    @Autowired
    private ICamStatusDao camStatusDao;

    @Autowired
    private IUploadLogDao uploadLogDao;

    private Gson gson = new Gson();

    private String imageDelePath;

    private String imageStorPath;

    private static BlockingQueue<String> imgDownloadQueue = new LinkedBlockingQueue<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(20);

    @PostConstruct
    public void init() {

        imageDelePath = configService.getConfig().get(Constants.CONFIG_IMAGE_DELETE_PATH, String.class);
        imageStorPath = configService.getConfig().get(Constants.CONFIG_IMAGE_STOR_PATH, String.class);

        //init downloadQueue
        File imageStorFile = new File(imageStorPath);
        Arrays.asList(imageStorFile.listFiles()).forEach(vidFile -> {
            Arrays.asList(vidFile.listFiles()).forEach(imageFile -> imgDownloadQueue.offer(imageFile.getName()));
        });

    }

    @Scheduled(cron="0 0 1,5 * * ?")
    private void cleanDeleFile() {
        try {
            File imgDeleFile = new File(imageDelePath);
            long size = FileUtils.sizeOfDirectory(imgDeleFile) / 1024 / 1024;
            logger.warn("the size of the file to be deleted is {} MB and the number of image to be deleted is {}",  size, imgDeleFile.listFiles().length);
            logger.info("start to clean image in {}", imageDelePath);
            long start = System.currentTimeMillis();
            FileUtils.cleanDirectory(imgDeleFile);
            long end = System.currentTimeMillis();
            logger.info("clean image in {} finished", imageDelePath);
            logger.warn("clean {} directory cost time : {} ", imageDelePath, end - start);
        } catch (Exception e) {
            logger.error("Exception happen when clean delete file.", e);
        }
    }

    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) {

        logger.info("Image uploaded with size {}", imageData.length);

        executorService.execute( () -> {
            try {
                long start = System.currentTimeMillis();
                FileUtils.writeByteArrayToFile(new File(imageStorPath + vehicleId + File.separator + vehicleId + "_" + timeStamp), imageData);
                imgDownloadQueue.offer(vehicleId + "_" + timeStamp);
                long end = System.currentTimeMillis();
                logger.warn("write image {} cost time : {} ", vehicleId + "_" + timeStamp, end - start);
            } catch (Exception e) {
                logger.error("Exception happen when write image to disk.", e);
            }
        });

        Map<String, Object> r = new HashMap<>();
        r.put("fileSize", imageData.length);
        r.put("success", true);
        return r;
    }

    @Override
    public Map<String, Object> downloadImage() {

        Map<String, Object> r = new HashMap<>();

        try {
            String imageName = imgDownloadQueue.poll();
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
        }

        return result;
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

    @Override
    public Map<String, Object> getFileStatus(String md5) {

        Map<String, Object> result = new HashMap<>();

        try {
            Integer storNum = 0;
            File imageStorFile = new File(imageStorPath);
            File[] files = imageStorFile.listFiles();
            for (File file : files) {
                storNum += file.listFiles().length;
            }
            logger.info("the number of images in the storage path is {}", storNum);

            Integer downloadNum = new File(imageDelePath).listFiles().length;
            logger.info("the number of images in the delete path is {}", downloadNum);

            List<String> abnormalImages = checkMd5(md5);
            logger.info("the number of images with abnormal md5  is {}", abnormalImages.size());

            result.put("storNum", storNum);
            result.put("downloadNum", downloadNum);
            result.put("abnormalImages", abnormalImages);


        } catch (Exception e) {
            logger.error("Exception happens when get files status for test : ", e);
        }

        return result;
    }

    @Override
    public Map<String, Object> camStatusReport(String requestId, String vehicleId, String service, String camera, String keyState, String resultCode, Long createTime) {
        Map<String, Object> result = new HashMap<>();

        int status = camStatusDao.insert(requestId, vehicleId, service, camera, keyState, resultCode, createTime);

        if (status == 1) {
            result.put("success", true);
            logger.info("insert camera status success");
        } else {
            result.put("success", false);
            logger.info("insert camera status failed with status code {}", status);
        }

        return result;
    }

    @Override
    public Map<String, Object> uploadLog(String requestId, String vehicleId, String service, String camera, String keyState, String resultCode, Long createTime, byte[] image, byte[] logFile) {
        Map<String, Object> result = new HashMap<>();

        if (image != null && image.length > 0) {
            s3Uploader.add(new LogFileInfo(image, true, vehicleId, requestId));
        }
        if (logFile != null && logFile.length > 0) {
            s3Uploader.add(new LogFileInfo(logFile, false, vehicleId, requestId));
        }

        int status = uploadLogDao.insert(requestId, vehicleId, service, camera, keyState, resultCode, createTime);
        if (status == 1) {
            result.put("success", true);
            logger.info("insert log info success");
        } else {
            result.put("success", false);
            logger.info("insert log info failed with status code {}", status);
        }

        return result;
    }

    private List<String> checkMd5(String md5) {

        List<String> result = new ArrayList<>();

        logger.info("to check image md5 in {}", imageStorPath);

        //check stor image md5
        File imageStorFile = new File(imageStorPath);
        Arrays.asList(imageStorFile.listFiles()).forEach(vidFile -> {
            Arrays.asList(vidFile.listFiles()).forEach(imageFile -> {
                try (InputStream is = Files.newInputStream(Paths.get(imageStorPath + imageFile.getName().split("_")[0] + File.separator + imageFile.getName()))) {
                    String imageMd5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
                    if (!imageMd5.equals(md5)) {
                        logger.error("image with abnormal md5 in {}, and the md5 value is {}", imageStorPath + imageFile.getName().split("_")[0] + File.separator + imageFile.getName(), imageMd5);
                        result.add(imageFile.getName());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });

        logger.info("to check image md5 in {}", imageDelePath);
        //check download image md5
        File imageDeleFile = new File(imageDelePath);
        Arrays.asList(imageDeleFile.listFiles()).forEach(file -> {
            try (InputStream is = Files.newInputStream( Paths.get(imageDelePath + File.separator + file.getName()))) {
                String imageMd5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(is);
                if (!imageMd5.equals(md5)) {
                    logger.error("image with abnormal md5 in {}, and the md5 value is {}", imageDelePath + File.separator + file.getName(), imageMd5);
                    result.add(file.getName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        return result;
    }
}
