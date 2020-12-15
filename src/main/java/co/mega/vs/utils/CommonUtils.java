package co.mega.vs.utils;


import java.util.HashMap;

public class CommonUtils {

    public static HashMap<Character, Integer> intList = new HashMap();

    public static Integer objectToInteger(Object o) {
        if (o != null) {
            if (o instanceof Integer) {
                return (Integer) o;
            } else if (o instanceof Number) {
                return ((Number) o).intValue();
            } else if (o instanceof String) {
                try {
                    return Integer.valueOf((String) o);
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
        return null;
    }

    public static Object getObjectByClassName(String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        return CommonUtils.class.getClassLoader().loadClass(className).newInstance();
    }

    public static Long objectToLong(Object o) {
        if (o != null) {
            if (o instanceof Long) {
                return (Long) o;
            } else if (o instanceof Number) {
                return ((Number) o).longValue();
            } else if (o instanceof String) {
                try {
                    return Long.valueOf((String) o);
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
        return null;
    }

    public static Double objectToDouble(Object o) {
        if (o != null) {
            if (o instanceof Double) {
                return (Double) o;
            } else if (o instanceof Number) {
                return ((Number) o).doubleValue();
            } else if (o instanceof String) {
                try {
                    return Double.valueOf((String) o);
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }
        return null;
    }
}

