package co.mega.vs.bean.impl;

import co.mega.vs.bean.IImageService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service("imageService")
public class ImageService implements IImageService {

    private static final String PATH = "/Users/sora/Desktop/";

    private static Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Override
    public Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) throws IOException {

        logger.info("Image uploaded with size {}", imageData.length);

        try {
            FileUtils.writeByteArrayToFile(new File(PATH + vehicleId + "_" + timeStamp), imageData);
        } catch (IOException e) {
            throw e;
        }

        Map<String, Object> r = new HashMap<>();
        r.put("fileSize", imageData.length);
        r.put("success", true);
        return r;
    }
}
