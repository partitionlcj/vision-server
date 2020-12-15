package co.mega.vs.bean.impl;

import co.mega.vs.bean.IHttpTool;
import co.mega.vs.bean.IImageService;
import co.mega.vs.entity.ImageStrategyResponse;
import co.mega.vs.utils.UrlUtils;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service("imageService")
public class ImageService implements IImageService {

    private static final String PATH = "/Users/h0153/Desktop/";

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private IHttpTool httpTool;

    @Autowired
    private UrlUtils urlUtils;

    private Gson gson = new Gson();

    private Map<String, Object> lockMap = new ConcurrentHashMap<>();

    private List<File> inDownloadingFiles = new ArrayList<>();

    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) throws IOException {

        logger.info("Image uploaded with size {}", imageData.length);

        try {
            Object lock = lockMap.computeIfAbsent(vehicleId, k -> new Object());
            synchronized (lock) {
                FileUtils.writeByteArrayToFile(new File(PATH + vehicleId + File.separator + vehicleId + "_" + timeStamp), imageData);
            }
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

        File file = findFile();
        if (file != null) {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            r.put("bytes", bytes);
            r.put("fileName", file.getName());
            removeFile(file);
        }

        return r;
    }

    private File findFile() {
//        File file = new File(PATH);
        File file = new File("/Users/h0153/Desktop/4567/");
        File[] vidFiles = file.listFiles(); // 该文件目录下全部vidFile

        if (vidFiles != null && vidFiles.length > 0) {
            for (int i = 0; i < vidFiles.length; i++) {
                Object lock = lockMap.computeIfAbsent(vidFiles[i].getName(), k -> new Object());
                synchronized (lock) {
                    File[] subFiles = vidFiles[i].listFiles();
                    if (subFiles != null && subFiles.length > 0) {
                        for (int j = 0; j < subFiles.length; j++) {
                            if (!inDownloadingFiles.contains(subFiles[j].getName())) {
                                inDownloadingFiles.add(subFiles[j]);
                                return subFiles[j];
                            }
                        }
                    } else {
                        continue;
                    }
                }
            }
        }

        return null;
    }

    private void removeFile (File file) {
        Object lock = lockMap.computeIfAbsent(file.getParentFile().getName(), k -> new Object());
        synchronized (lock) {
            inDownloadingFiles.remove(file);
            file.delete();
        }
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

        try {
            ImageStrategyResponse imageStrategyResponse = gson.fromJson(rtJson, ImageStrategyResponse.class);
            if (imageStrategyResponse.resultCode != null && imageStrategyResponse.resultCode.equals("success") && imageStrategyResponse.data != null) {
                if (Boolean.FALSE.equals(imageStrategyResponse.data.isDirected)) {
                    result.put("frequency", 60);
                    result.put("dailyLimit", 120);
                    result.put("displacement", 20);
                    result.put("retryInterval", 15);
                    result.put("retryLimit", 10);
                } else {
                    // 定向数据结构待定
                }
            }
        } catch (Exception e) {
            logger.warn("Error when parsing image strategy response: ", e);
        }
        return result;
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
