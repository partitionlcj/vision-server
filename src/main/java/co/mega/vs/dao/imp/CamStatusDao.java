package co.mega.vs.dao.imp;

import co.mega.vs.dao.ICamStatusDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service("camStatusDao")
public class CamStatusDao implements ICamStatusDao {

    private static Logger logger = LoggerFactory.getLogger(CamStatusDao.class);

    private String insertSql = "INSERT INTO camera_status_info (" +
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
            logger.info("insert into table camera_status_info, requestId : {}, vehicleId : {}, service : {}, camera : {}, keyState : {}, resultCode : {}, createTime : {}", requestId, vehicleId, service, camera, keyState, resultCode, createTime);
        } catch (Exception e) {
            logger.error("Exception happens when insert data into table camera_status_info", e);
        }
        return status;
    }
}
