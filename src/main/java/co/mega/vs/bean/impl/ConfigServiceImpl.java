package co.mega.vs.bean.impl;

import co.mega.vs.config.IConfigService;
import co.mega.vs.config.impl.Config;
import co.mega.vs.config.impl.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("configService")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConfigServiceImpl implements IConfigService {

    private Config config;

    public static final Logger logger = LoggerFactory.getLogger(ConfigServiceImpl.class);

    @Override
    @PostConstruct
    public void loadConfig() {
        // We do not use specified config file now. We search it in classpath.
        if (config == null) {
            config = ConfigUtils.getConfig();
        }
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

}
