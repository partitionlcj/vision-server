package co.mega.vs.config.impl;


import co.mega.vs.utils.CommonUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Config implements Serializable {

    private static final long serialVersionUID = 5053543569976829511L;

    private Map<String, Object> configMap;

    public Config(Map<String, Object> configMap) {
        if (configMap != null) {
            this.configMap = configMap;
        } else {
            this.configMap = new HashMap<>();
        }
    }

    public Object get(String key) {
        return configMap.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object o = get(key);

        if (o == null) {
            return null;
        }

        if (Integer.class.isAssignableFrom(clazz)) {
            return (T) CommonUtils.objectToInteger(o);
        }

        if (Long.class.isAssignableFrom(clazz)) {
            return (T) CommonUtils.objectToLong(o);
        }

        if (Double.class.isAssignableFrom(clazz)) {
            return (T) CommonUtils.objectToDouble(o);
        }

        try {
            return (T) o;
        } catch (Exception e) {
            return null;
        }
    }

}
