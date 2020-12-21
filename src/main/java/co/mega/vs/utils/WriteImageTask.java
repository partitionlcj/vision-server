package co.mega.vs.utils;

import co.mega.vs.entity.ImageInfo;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

public class WriteImageTask implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(WriteImageTask.class);

    private String imageStorPath;

    private ImageInfo imageInfo;

    private LinkedBlockingQueue<String> imageDownloadQueue;

    public WriteImageTask(String imageStorPath, ImageInfo imageInfo, LinkedBlockingQueue<String> imageDownloadQueue) {
        this.imageStorPath = imageStorPath;
        this.imageInfo = imageInfo;
        this.imageDownloadQueue = imageDownloadQueue;
    }

    @Override
    public void run() {
        try {
            long start = System.currentTimeMillis();
            FileUtils.writeByteArrayToFile(new File(imageStorPath + imageInfo.getVid() + File.separator + imageInfo.getVid() + "_" + imageInfo.getTimeStamp()), imageInfo.getImageData());
            imageDownloadQueue.offer(imageInfo.getVid() + "_" + imageInfo.getTimeStamp());
            long end = System.currentTimeMillis();
            logger.warn("write image {} cost time : {} ", imageInfo.getVid() + "_" + imageInfo.getTimeStamp(), end - start);
        } catch (IOException e) {
            logger.error("Exception happen when write image to disk.", e);
        }
    }
}
