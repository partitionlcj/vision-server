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
        OTHER
    }
}