package co.mega.vs.utils;

public class Constants {

    // Configuration entry keys
    public static final String CONFIG_VALIDATORS = "validators";

    // Project constants
    public static final String DEFAULT_CONFIG_FILE_NAME = "config.json";

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String TSP_URL = "tspUrl";

    public static final String CONFIG_ENV = "env";

    // Http client
    public static final String CONFIG_MAX_TOTAL = "maxTotal";
    public static final String CONFIG_DEFAULT_MAX_PER_ROUTE = "defaultMaxPerRoute";
    public static final String CONFIG_CONNECT_TIMEOUT = "connectTimeout"; // 建立连接的超时
    public static final String CONFIG_SOCKET_TIMEOUT = "socketTimeout"; // 读取数据的超时
    public static final String CONFIG_CONNECTION_REQUEST_TIMEOUT = "connectionRequestTimeout"; // 从连接池中获取连接的超时

    public static final String APP_SECRET = "appSecret";

    public static final String CONFIG_IMAGE_STOR_PATH = "imageStorPath";
    public static final String CONFIG_IMAGE_DELETE_PATH = "imageDelePath";

    //s3
    public static String AWS_S3_ACCESS_KEY = "AKIATIWY752LZ6OCX5DD";
    public static String AWS_S3_SECRET_KEY = "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";

    public static String HW_OBS_ACCESS_KEY = "EPYG0BBE0O37MORZ8WGE";
    public static String HW_OBS_SECRET_KEY = "BFoQLLDiBELjZMndiRLj8ZprF4XoKDMBHqYZO70R";
    public static String HW_OBS_ENDPOINT_URL = "http://obs.cn-south-1.myhuaweicloud.com";

    public static String AWS_BUCKET_NAME_PREFIX = "ais-storage";
    public static String GN_BUCKET_NAME_PREFIX = "ais-storage-gz";

    public static final String CONFIG_LOD_FILE_QUEUE_SIZE = "logFileQueueSize";

    //uploadImage
    public static final String UPLOAD_IMAGE_COUNT = "upload_image_count";

}
