package co.mega.vs.dao;

public interface ICamStatusDao {

    int insert(String requestId, String vehicleId, String service, String camera, String keyState, String resultCode, Long createTime);
}
