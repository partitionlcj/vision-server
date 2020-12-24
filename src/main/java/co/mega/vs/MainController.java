package co.mega.vs;

import co.mega.vs.bean.IImageService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class MainController {
    private static Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    IImageService imageService;

    @PostConstruct
    public void init() {
    }

    @PostMapping(value = {"/vs/img/upload"})
    public ResponseEntity imageUpload(HttpServletRequest request, @RequestParam(value = "req_id", required = false) String requestId, @RequestParam(value = "vid", required = false) String vehicleId, @RequestParam(value = "image", required = false) MultipartFile image) {
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("reqId", requestId);

        logger.info("-------------------------------------------------------------------------------");
        logger.info("Request for image upload");

        Map<String, Object> result;
        try {
            if (StringUtils.isBlank(vehicleId)) {
                throw new InvalidParameterException("vid should not be blank!");
            }

            if (image == null || image.getBytes() == null || image.getBytes().length == 0) {
                throw new InvalidParameterException("image should not be blank!");
            }

            logger.info("requestId is {}, vehicleId is {}", requestId, vehicleId);

            Map<String, Object> data = imageService.uploadImage(vehicleId, image.getOriginalFilename(), image.getBytes());
            result = generateResult(requestId, "success", null);
            result.put("data", data);
        } catch (InvalidParameterException e) {
            logger.error("Invalid param: ", e);
            result = generateResult(requestId, "invalid_param", e.getMessage());
            return new ResponseEntity(result, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception happens in workflow: ", e);
            result = generateResult(requestId, "internal_error", e.getMessage());
            return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            logger.info("-------------------------------------------------------------------------------");
        }

        MDC.remove("reqId");

        return new ResponseEntity(result, HttpStatus.OK);
    }

    private Map<String, Object> generateResult(String reqId, String resultCode, String debugMessage) {
        Map<String, Object> result = new HashMap<>();
        result.put("request_id", reqId);
        result.put("server_time", System.currentTimeMillis() / 1000);
        result.put("result_code", resultCode);
        if (StringUtils.isNotBlank(debugMessage)) {
            result.put("debug_message", debugMessage);
        }
        return result;
    }

    @GetMapping(value = "/vs/img/st")
    public ResponseEntity imageStrategy(HttpServletRequest request, @RequestParam(value = "req_id", required = false) String requestId, @RequestParam(value = "vid", required = false) String vehicleId, @RequestParam(value = "iccid", required = false) String iccId) {
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("reqId", requestId);

        logger.info("-------------------------------------------------------------------------------");
        logger.info("Request for image strategy search");

        Map<String, Object> result;
        try {
            if (StringUtils.isBlank(vehicleId) || StringUtils.isBlank(iccId)) {
                throw new InvalidParameterException("vid or iccid should not be blank!");
            }

            logger.info("requestId is {}, vehicleId is {}, iccId is {}", requestId, vehicleId, iccId);

            Map<String, Object> data = imageService.imageStrategy(iccId);

            result = generateResult(requestId, "success", null);
            result.put("data", data);
        } catch (InvalidParameterException e) {
            logger.error("Invalid param: ", e);
            result = generateResult(requestId, "invalid_param", e.getMessage());
            return new ResponseEntity(result, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception happens in workflow: ", e);
            result = generateResult(requestId, "internal_error", e.getMessage());
            return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            logger.info("-------------------------------------------------------------------------------");
        }

        MDC.remove("reqId");

        return new ResponseEntity(result, HttpStatus.OK);
    }


    @GetMapping(value = "/vs/img/download")
    public ResponseEntity downloadImage(HttpServletRequest request, @RequestParam(value = "req_id", required = false) String requestId) {
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("reqId", requestId);

        logger.info("-------------------------------------------------------------------------------");
        logger.info("Request for image download");

        Map<String, Object> result;
        try {
            Map<String, Object> data = imageService.downloadImage();

            if (data.containsKey("fileName") && data.containsKey("bytes")) {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.add("fileName", (String) data.get("fileName"));
                return new ResponseEntity(data.get("bytes"), httpHeaders, HttpStatus.OK);
            } else {
                logger.error("not found image, please try again later");
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }

        } catch (InvalidParameterException e) {
            logger.error("Invalid param: ", e);
            result = generateResult(requestId, "invalid_param", e.getMessage());
            return new ResponseEntity(result, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception happens in workflow: ", e);
            result = generateResult(requestId, "internal_error", e.getMessage());
            return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            MDC.remove("reqId");
            logger.info("-------------------------------------------------------------------------------");
        }

    }

    //for test
    @GetMapping(value = {"/vs/img/test/status"})
    public ResponseEntity fileStatus(HttpServletRequest request, @RequestParam(value = "req_id", required = false) String requestId, @RequestParam(value = "md5", required = false) String md5) {
        if (StringUtils.isBlank(requestId)) {
            requestId = UUID.randomUUID().toString();
        }

        MDC.put("reqId", requestId);

        logger.info("-------------------------------------------------------------------------------");
        logger.info("Request for get file status for test");

        Map<String, Object> result;
        try {
            if (StringUtils.isBlank(md5)) {
                throw new InvalidParameterException("md5 should not be blank!");
            }

            logger.info("requestId is {}, md5 is {}", requestId, md5);

            Map<String, Object> data = imageService.getFileStatus(md5);
            result = generateResult(requestId, "success", null);
            result.put("data", data);
        } catch (InvalidParameterException e) {
            logger.error("Invalid param: ", e);
            result = generateResult(requestId, "invalid_param", e.getMessage());
            return new ResponseEntity(result, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Exception happens in workflow: ", e);
            result = generateResult(requestId, "internal_error", e.getMessage());
            return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            logger.info("-------------------------------------------------------------------------------");
        }

        MDC.remove("reqId");

        return new ResponseEntity(result, HttpStatus.OK);
    }
}
