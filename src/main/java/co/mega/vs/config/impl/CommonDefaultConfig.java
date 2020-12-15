package co.mega.vs.config.impl;


import co.mega.vs.config.IDefaultConfig;
import co.mega.vs.utils.Constants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommonDefaultConfig implements IDefaultConfig {

    private Map<String, Object> defaultConf;

    public CommonDefaultConfig() {
        defaultConf = new HashMap<String, Object>() {
            {
                put(Constants.CONFIG_VALIDATORS,
                        Collections.singletonList("co.mega.ds.config.impl.CommonConfigValidator")
                );

                // HttpClient config
                put(Constants.CONFIG_MAX_TOTAL, 500);
                put(Constants.CONFIG_DEFAULT_MAX_PER_ROUTE, 50);
                put(Constants.CONFIG_CONNECT_TIMEOUT, 500);
                put(Constants.CONFIG_SOCKET_TIMEOUT, 1300);
                put(Constants.CONFIG_CONNECTION_REQUEST_TIMEOUT, 500);

                put(Constants.APP_ID, "100030000");

            }
        };
    }

    @Override
    public Map<String, Object> getConfigDefaultValues() {
        return defaultConf;
    }
}
