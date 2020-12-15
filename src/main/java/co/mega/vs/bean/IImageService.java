package co.mega.vs.bean;

import java.io.IOException;
import java.util.Map;

public interface IImageService {
    Map<String, Object> uploadImage(String vehicleId, String timeStamp, byte[] imageData) throws IOException;

    Map<String, Object> downloadImage() throws IOException;

    Map<String, Object> imageStrategy(String vehicleId);
}
