package co.mega.vs.dao;

public interface IUploadLogDao {

    int insert(String requestId, String vehicleId, String service, String camera, String keyState, String resultCode, Long createTime);
}
