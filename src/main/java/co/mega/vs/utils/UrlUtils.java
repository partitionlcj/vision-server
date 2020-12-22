package co.mega.vs.utils;

import co.mega.vs.config.IConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component("urlUtils")
public class UrlUtils {

    private static Logger logger = LoggerFactory.getLogger(UrlUtils.class);

    @Autowired
    private IConfigService configService;

    private String tspUrl;

    @PostConstruct
    public void init() {
        tspUrl = configService.getConfig().get(Constants.TSP_URL, String.class);
        logger.info("tspUrl is {}", tspUrl);
    }

    public static String IMAGE_STRATEGY_URL_END_POINT = "/api/1/in/mno/mc/flow/info";

    public String getImageStrategyUrl() {
        return tspUrl + IMAGE_STRATEGY_URL_END_POINT;
    }
}
