package co.mega.vs.bean.impl;

/**
 */
public class QueryUrl {
    String url;
    String originalUrl;
    QueryType type;

    public QueryUrl(String url, QueryType type) {
        this.url = url;
        this.type = type;
        this.originalUrl = url;
    }


    // 用于区分外部服务调用失败metric监控，如果不需要可以设置成OTHER
    public enum QueryType {
        // outside service api
        XIMA_MEDIA_SEARCH,
        QQ_MEDIA_SEARCH,
        ACCOUNT_SERVICE,
        VEHICLE_API,
        CONTACT_SERVICE,
        MEDIA_STATUS_SYNC,
        NAVI_API,
        NAVI_STATUS_SYNC,
        WEATHER_SERVER,
        DRIVER_PROFILE,
        INCREMENT_DRIVER_PROFILE,
        PERMISSION_SERVICE,

        // internal api
        NLP_SERVER,
        MODEL_SERVER,
        ASR_CORRECTION_SERVER,
        MR_MODEL_SERVER,
        DST_SERVER,
        QUERYRW_SERVER,
        CHAT_SERVER,
        QA_SERVER,
        SOUGO_NLU,
        IFLY_NLU,
        OTHER
    }
}

