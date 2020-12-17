package co.mega.vs.config.impl;

import co.mega.vs.config.IDefaultConfig;
import co.mega.vs.utils.CommonUtils;
import co.mega.vs.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConfigUtils {
    private static Logger logger = LoggerFactory.getLogger(ConfigUtils.class);

    public static List<String> defaultConfigClasses = Arrays.asList("co.mega.vs.config.impl.CommonDefaultConfig");

    private static Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    public static Config getConfig() {
        Map<String, Object> configMap = new HashMap<>();
        String jsonConfigFileStr = null;

        try {
            // load the default config file in classpath
            jsonConfigFileStr = IOUtils.toString(
                    ConfigUtils.class.getClassLoader().getResourceAsStream(Constants.DEFAULT_CONFIG_FILE_NAME),
                    Constants.DEFAULT_ENCODING);

            Type type = new TypeToken<Map<String, Object>>() {
            }.getType();
            Map<String, Object> inputConfigFileMap = gson.fromJson(jsonConfigFileStr, type);

            // Collect default values
            try {
                Map<String, Object> defaultConfigMap = collectDefaultConfigValues(defaultConfigClasses);
                configMap.putAll(defaultConfigMap);
            } catch (Exception e) {
                logger.warn("Sign error : {}", e);
                // Do nothing if no default values
            }

            configMap.putAll(inputConfigFileMap);
            Config config = new Config(configMap);
            return config;
        } catch (Exception e) {
            logger.warn("Sign error : {}", e);
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Object> collectDefaultConfigValues(List<String> defaultConfigClassNames)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Map<String, Object> defaultConfigValues = new HashMap<>();
        for (String className : defaultConfigClassNames) {
            IDefaultConfig defaultConfig = (IDefaultConfig) (CommonUtils.getObjectByClassName(className));
            // The default classes coming later would overwrite the default value of ones coming earlier, if the config key is the same
            defaultConfigValues.putAll(defaultConfig.getConfigDefaultValues());
        }

        return defaultConfigValues;
    }
}
