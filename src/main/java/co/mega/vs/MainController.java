package co.mega.vs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class MainController {
    private static Logger logger = LoggerFactory.getLogger(MainController.class);

    @PostConstruct
    public void init() {
    }

    @PostMapping(value = {"/vs/img/upload"})
    public ResponseEntity imageUpload(HttpServletRequest request) {
        logger.info("-------------------------------------------------------------------------------");
        logger.info("Request for image upload");

        Map<String, Object> result = new HashMap<>();
        ResponseEntity response = new ResponseEntity(result, null, HttpStatus.OK);

        logger.info("-------------------------------------------------------------------------------");

        return response;
    }
}
