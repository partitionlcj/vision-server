package co.mega.vs.bean;

import co.mega.vs.bean.impl.QueryUrl;
import org.apache.http.entity.ContentType;

import java.util.Map;

public interface IHttpTool {

	String queryPostJson(String queryJson, QueryUrl url);

	String queryPostJson(String queryJson, QueryUrl queryUrl, boolean needSign, String vehicleId);

	String queryGet(QueryUrl url);

	String queryGet(QueryUrl url, boolean isSign, String vehicleId);

	String queryGet(QueryUrl url, String token);

	String queryGet(QueryUrl queryUrl, String token, boolean isSign, String vehicleId);
	String queryGet(QueryUrl queryUrl, String token, boolean isSign);

	String queryPostFormUrlEncoded(Map<String, String> params, QueryUrl url);

	String queryPostFormUrlEncoded(Map<String, String> params, QueryUrl url, boolean isSign, String vehicleId);

	String queryPostBinary(QueryUrl queryUrl, String token, ContentType contentType, boolean withBearer, String sign,
                           Map<String, String> headers, byte[] binaryBody);
}
