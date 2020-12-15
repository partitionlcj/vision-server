package co.mega.vs.bean.impl;

import co.mega.vs.bean.IHttpTool;
import co.mega.vs.config.IConfigService;
import co.mega.vs.utils.CloudUtils;
import co.mega.vs.utils.Constants;
import co.mega.vs.utils.UrlUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service("httpTool")
public class HttpToolImpl implements IHttpTool {

    private static final Logger logger = LoggerFactory.getLogger(HttpToolImpl.class);

    private PoolingHttpClientConnectionManager cm;

    private CloseableHttpClient httpClient;

    private IdleConnectionMonitorThread staleMonitor;

    @Autowired
    private IConfigService configService;

    @Autowired
    private UrlUtils urlUtils;

    private JsonParser jsonParser = new JsonParser();

    @PostConstruct
    public void init() {
        // Initialize connection impl
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(configService.getConfig().get(Constants.CONFIG_MAX_TOTAL, Integer.class));
        cm.setDefaultMaxPerRoute(configService.getConfig().get(Constants.CONFIG_DEFAULT_MAX_PER_ROUTE, Integer.class));

        staleMonitor = new IdleConnectionMonitorThread(cm);
        staleMonitor.start();

        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(configService.getConfig().get(Constants.CONFIG_CONNECT_TIMEOUT, Integer.class))
                .setSocketTimeout(configService.getConfig().get(Constants.CONFIG_SOCKET_TIMEOUT, Integer.class))
                .setConnectionRequestTimeout(configService.getConfig().get(Constants.CONFIG_CONNECTION_REQUEST_TIMEOUT, Integer.class)).build();
        httpClient = HttpClientBuilder.create().setConnectionManager(cm).setDefaultRequestConfig(config)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(1, false)).build();

        logger.info("socketTimeout {}", config.getSocketTimeout());
    }

    // Used to post json
    @Override
    public String queryPostJson(String queryJson, QueryUrl queryUrl) {
        return doHttpRequest(queryJson, queryUrl, "POST", null, ContentType.APPLICATION_JSON, null);
    }

    @Override
    public String queryPostJson(String queryJson, QueryUrl queryUrl, boolean needSign, String vehicleId) {
        String sign = null;
        if (needSign) {
            try {
                URIBuilder uriBuilder = new URIBuilder(queryUrl.originalUrl);
                uriBuilder.addParameter("app_id", configService.getConfig().get(Constants.APP_ID, String.class));
                uriBuilder.addParameter("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

                List<NameValuePair> p = new ArrayList<>();
                p.addAll(uriBuilder.getQueryParams());

                p.add(new BasicNameValuePair("jsonBody", queryJson));

                sign = CloudUtils.signURLAndRequestParams("POST", uriBuilder.getPath(), p,
                        configService.getConfig().get(Constants.APP_SECRET, String.class), null, null);

                queryUrl.url = uriBuilder.toString();
            } catch (URISyntaxException e) {
                logger.warn("URI error : {}", e);
                return null;
            } catch (NoSuchAlgorithmException e) {
                logger.warn("Sign error : {}", e);
                return null;
            }
        }

        return doHttpRequest(queryJson, queryUrl, "POST", null, ContentType.APPLICATION_JSON, null, false, sign, null, null);
    }

    // Used for GET
    @Override
    public String queryGet(QueryUrl url) {
        return queryGet(url, false, null);
    }

    @Override
    public String queryGet(QueryUrl queryUrl, boolean isSign, String vehicleId) {
        return queryGet(queryUrl, null, isSign, vehicleId);
    }

    // Used for GET with token in header
    @Override
    public String queryGet(QueryUrl queryUrl, String token) {
        return doHttpRequest(null, queryUrl, "GET", token, null, null);
    }

//    queryGet(QueryUrl queryUrl, String token, boolean isSign, CarType carType)
    @Override
    public String queryGet(QueryUrl queryUrl, String token, boolean isSign) {
        String sign = null;
        if (isSign) {
            try {
                URIBuilder uriBuilder = new URIBuilder(queryUrl.originalUrl);
                uriBuilder.addParameter("nonce", String.valueOf(UUID.randomUUID().getMostSignificantBits()));
                uriBuilder.addParameter("app_id", configService.getConfig().get(Constants.APP_ID, String.class));
                uriBuilder.addParameter("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

                sign = CloudUtils.signURLAndRequestParams("GET", uriBuilder.getPath(), uriBuilder.getQueryParams(),
                        configService.getConfig().get(Constants.APP_SECRET, String.class), null, null);


                queryUrl.url = uriBuilder.toString();
            } catch (URISyntaxException e) {
                logger.warn("URI error : {}", e);
                return null;
            }  catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return doHttpRequest(null, queryUrl, "GET", token, null, null, true, sign, null, null);
    }

    @Override
    public String queryGet(QueryUrl queryUrl, String token, boolean isSign, String vehicleId) {
//       return queryGet(queryUrl, token, isSign, CarType);
        return queryGet(queryUrl, token, isSign);
    }

    // Used for urlencoded form
    @Override
    public String queryPostFormUrlEncoded(Map<String, String> params, QueryUrl url) {
        return doHttpRequest(null, url, "POST", null, ContentType.APPLICATION_FORM_URLENCODED, params);
    }

    @Override
    public String queryPostFormUrlEncoded(Map<String, String> params, QueryUrl url, boolean isSign, String vehicleId) {
        String sign = null;
        if (isSign) {
            try {
                URIBuilder uriBuilder = new URIBuilder(url.originalUrl);
                uriBuilder.addParameter("app_id", configService.getConfig().get(Constants.APP_ID, String.class));
                uriBuilder.addParameter("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

                List<NameValuePair> p = new ArrayList<>();
                p.addAll(uriBuilder.getQueryParams());

                for (Map.Entry<String, String> entry : params.entrySet()) {
                    p.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }

                sign = CloudUtils.signURLAndRequestParams("POST", uriBuilder.getPath(), p,
                        configService.getConfig().get(Constants.APP_SECRET, String.class), null, null);

                url.url = uriBuilder.toString();
            } catch (URISyntaxException e) {
                logger.warn("URI error : {}", e);
                return null;
            } catch (NoSuchAlgorithmException e) {
                logger.warn("Sign error : {}", e);
                return null;
            }
        }

        return doHttpRequest(null, url, "POST", null, ContentType.APPLICATION_FORM_URLENCODED, params, true, sign, null, null);
    }

    @Override
    public String queryPostBinary(QueryUrl queryUrl, String token, ContentType contentType, boolean withBearer, String sign,
                                  Map<String, String> headers, byte[] binaryBody) {
        return doHttpRequest(null, queryUrl, "POST", token, contentType, null, withBearer, sign, headers, binaryBody);
    }

    public void shutDown() {
        if (staleMonitor != null) {
            staleMonitor.shutdown();
        }
        if (httpClient != null) {
            HttpClientUtils.closeQuietly(httpClient);
        }
        if (cm != null) {
            cm.close();
        }
    }

    private String doHttpRequest(String queryJson, QueryUrl queryUrl, String method, String token, ContentType contentType, Map<String, String> params, boolean withBearer, String sign,
                                 Map<String, String> headers, byte[] binaryBody) {
        long startTime = System.currentTimeMillis();

        String responseJson = "";
        String url = queryUrl.url;
        logger.debug("query url: " + url);
        if (queryJson != null) {
            logger.debug("query body: " + queryJson);
        }

        CloseableHttpResponse response = null;
        boolean querySuccess = false;
        int statusCode = -1;

        try {
            HttpUriRequest request;
            if (method.equals("POST")) {
                request = new HttpPost(url);

                if (contentType.equals(ContentType.APPLICATION_JSON)) {
                    StringEntity body = new StringEntity(queryJson, contentType);
                    ((HttpPost) request).setEntity(body);
                } else if (contentType.equals(ContentType.APPLICATION_FORM_URLENCODED)) {
                    if (params != null) {
                        logger.debug("post params : {}", params);
                        List<NameValuePair> urlParameters = new ArrayList<>();
                        params.entrySet().stream().forEach(entry -> urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue())));
                        try {
                            ((HttpPost) request).setEntity(new UrlEncodedFormEntity(urlParameters, Constants.DEFAULT_ENCODING));
                        } catch (UnsupportedEncodingException e) {
                            logger.warn("Error happens when building http post:", e);
                        }
                    } else {
                        StringEntity body = new StringEntity("", contentType);
                        ((HttpPost) request).setEntity(body);
                    }
                } else if (contentType.equals(ContentType.APPLICATION_OCTET_STREAM)) {
                    if (binaryBody != null) {
                        ByteArrayEntity body = new ByteArrayEntity(binaryBody);
                        ((HttpPost) request).setEntity(body);
                    }
                }
            } else {
                request = new HttpGet(url);
            }

            if (StringUtils.isNotBlank(sign)) {
                request.addHeader("KK-Sign", sign);
            }

            if (StringUtils.isNotBlank(token)) {
                if (withBearer) {
                    request.addHeader("Authorization", "Bearer " + token);
                } else {
                    request.addHeader("Authorization", token);
                }
            }

            if (headers != null) {
                headers.entrySet().forEach(e -> request.addHeader(e.getKey(), e.getValue()));
            }

            if (QueryUrl.QueryType.CHAT_SERVER.equals(queryUrl.type) || QueryUrl.QueryType.QA_SERVER.equals(queryUrl.type) || QueryUrl.QueryType.SOUGO_NLU.equals(queryUrl.type) || QueryUrl.QueryType.IFLY_NLU.equals(queryUrl.type)) {
                RequestConfig config = RequestConfig.custom()
                        .setConnectTimeout(configService.getConfig().get(Constants.CONFIG_CONNECT_TIMEOUT, Integer.class))
                        .setSocketTimeout(configService.getConfig().get(Constants.CONFIG_SOCKET_TIMEOUT_FOR_QA, Integer.class))
                        .setConnectionRequestTimeout(configService.getConfig().get(Constants.CONFIG_CONNECTION_REQUEST_TIMEOUT, Integer.class)).build();
                ((HttpRequestBase) request).setConfig(config);
            }
            response = httpClient.execute(request);

            statusCode = response.getStatusLine().getStatusCode();
            HttpEntity resultEntity = response.getEntity();
            if (resultEntity != null) {
                responseJson = EntityUtils.toString(resultEntity, Constants.DEFAULT_ENCODING);
            }

            if (statusCode == HttpStatus.SC_OK) {
                // we don't monitor url of OTHER type
                if (QueryUrl.QueryType.OTHER.equals(queryUrl.type) || QueryUrl.QueryType.SOUGO_NLU.equals(queryUrl.type) || QueryUrl.QueryType.IFLY_NLU.equals(queryUrl.type)) {
                    querySuccess = true;
                } else {
                    JsonElement element = jsonParser.parse(responseJson);
                    JsonElement resultCodeElm = element.getAsJsonObject().get("result_code");
                    String resultCodeStr = resultCodeElm.getAsString();
                    if (resultCodeElm != null && "success".equals(resultCodeStr)) {
                        querySuccess = true;
                    }

                    // deal with special case
                    if (!querySuccess) {
                        switch (queryUrl.type) {
                            case ACCOUNT_SERVICE:
                                if ("auth_failed".equals(resultCodeStr)) {
                                    querySuccess = true;
                                }
                                break;
                            case VEHICLE_API:
                            case DRIVER_PROFILE:
                                if ("resource_not_found".equals(resultCodeStr)) {
                                    querySuccess = true;
                                }
                                break;
                            case NAVI_STATUS_SYNC:
                                if ("null_response".equals(resultCodeStr)) {
                                    querySuccess = true;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }

                if (!QueryUrl.QueryType.MODEL_SERVER.equals(queryUrl.type) && !QueryUrl.QueryType.MR_MODEL_SERVER.equals(queryUrl.type)) {
                    logger.info("jsonReturned: " + responseJson);
                }
            }
        } catch (ConnectException e) {
            logger.warn("Error connecting server: ", e);
        } catch (ConnectTimeoutException e) {
            logger.warn("Error connecting server: ", e);
        } catch (SocketTimeoutException e) {
            logger.info("Rest call timeout: ", e);
        } catch (Exception e) {
            logger.error("QuerySearcher error: ", e);
        } finally {
            // for monitoring outside service
            if (response == null || !querySuccess) {
                logger.error("outside service failure, query url: {}, status code: {}, response message: {}", queryUrl.url, statusCode, responseJson);
                switch (queryUrl.type) {
                    case XIMA_MEDIA_SEARCH:
                    case ACCOUNT_SERVICE:
                    case VEHICLE_API:
                    case CONTACT_SERVICE:
                    case MEDIA_STATUS_SYNC:
                    case NAVI_API:
                    case NAVI_STATUS_SYNC:
                    case WEATHER_SERVER:
                    case DRIVER_PROFILE:
                    case PERMISSION_SERVICE:
                        break;
                }
            }

            if (response != null) {
                try {
                    EntityUtils.consume(response.getEntity());
                    response.close();
                } catch (IOException e) {
                }
            }
        }

        logger.warn("Http call time cost is {}, query url : {}", System.currentTimeMillis() - startTime, url);

        return responseJson;
    }

    private String doHttpRequest(String queryJson, QueryUrl queryUrl, String method, String token, ContentType contentType, Map<String, String> params) {
        return doHttpRequest(queryJson, queryUrl, method, token, contentType, params, true, null, null, null);
    }

    class IdleConnectionMonitorThread extends Thread {
        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown = false;

        public IdleConnectionMonitorThread
                (PoolingHttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            while (!shutdown) {
                try {
                    synchronized (this) {
                        wait(30 * 1000);
                        connMgr.closeExpiredConnections();
                        connMgr.closeIdleConnections(60, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException ex) {
                }
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }
    }


}
