package co.mega.vs.entity;

public class LogFileInfo {

    private byte[] file;

    private boolean isImage;

    private String vehicleId;

    private String requestId;

    public LogFileInfo(byte[] file, boolean isImage, String vehicleId, String requestId) {
        this.file = file;
        this.isImage = isImage;
        this.vehicleId = vehicleId;
        this.requestId = requestId;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setImage(boolean image) {
        isImage = image;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
