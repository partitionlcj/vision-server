package co.mega.vs.bean;

import java.util.Map;

public interface IImageService {
    Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData);

    Map<String, Object> downloadImage();

    Map<String, Object> imageStrategy(String vehicleId);

    Map<String, Object> getFileStatus(String md5);
}
