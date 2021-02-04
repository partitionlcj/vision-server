package co.mega.vs.dao.imp;

import co.mega.vs.dao.IUploadLogDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("uploadLogDao")
public class UploadLogDao implements IUploadLogDao {

    private static Logger logger = LoggerFactory.getLogger(UploadLogDao.class);

    private String insertSql = "INSERT INTO vs_log_info (" +
            "id, vehicle_id, service, cam_name, key_state, result_code, create_time)" +
            "VALUES (" +
            "?, ?, ?, ?, ?, ?, ?)";

    @Autowired
    @Qualifier("mainJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Override
    public int insert(String requestId, String vehicleId, String service, String camera, String keyState, String resultCode, Long createTime) {
        int status = 0;
        try {
            status = jdbcTemplate.update(insertSql, new Object[]{requestId, vehicleId, service, camera, keyState, resultCode, createTime});
            logger.info("insert into table vs_log_info, requestId : {}, vehicleId : {}, service : {}, camera : {}, keyState : {}, resultCode : {}, createTime : {}", requestId, vehicleId, service, camera, keyState, resultCode, createTime);
        } catch (Exception e) {
            logger.error("Exception happens when insert data into table vs_log_info", e);
        }
        return status;
    }
}
