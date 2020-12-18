package co.mega.vs.entity;

public class ImageInfo {

    private String vid;

    private String timeStamp;

    private byte[] imageData;

    public ImageInfo(String vid, String timeStamp, byte[] imageData) {
        this.vid = vid;
        this.timeStamp = timeStamp;
        this.imageData = imageData;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public byte[] getImageData() {
        return imageData;
    }

    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }
}
