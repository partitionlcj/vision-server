package co.mega.vs.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class CloudUtils {
    private static Logger logger = LoggerFactory.getLogger(CloudUtils.class);

    public static String linkSortedNoneEmptyArgs(List<NameValuePair> params, String... excludeKeys) {
        if (null == params || params.isEmpty()) {
            return "";
        }

        params.sort((a, b) -> {
            if (a.getName().equals(b.getName())) {
                return a.getValue().compareTo(b.getValue());
            }
            return a.getName().compareTo(b.getName());
        });

        List<String> exclude = null == excludeKeys ? Collections.emptyList() : Arrays.asList(excludeKeys);

        StringBuilder sb = new StringBuilder();
        params.stream()
                .filter(p -> !exclude.contains(p.getName()) && !StringUtils.isEmpty(p.getValue()))
                .forEach(p -> sb.append(p.getName()).append("=").append(p.getValue()).append("&"));

        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }


    public static String signURLAndRequestParams(String method, String path, List<NameValuePair> params, String appSecret, String accessToken, String scopedIn) throws NoSuchAlgorithmException {
        StringBuilder sign = new StringBuilder().append(method).append(path);
        String paramString = linkSortedNoneEmptyArgs(params, new String[]{});
        sign.append("?").append(StringUtils.isBlank(paramString) ? "" : paramString).append(appSecret);

        if (accessToken != null) {
            sign.append(accessToken);
        }

        if (scopedIn != null) {
            sign.append(scopedIn);
        }
        return md5(sign.toString());
    }


    public static String signURLAndRequestParams(String path, List<String> params, String appSecret, String method, String accessToken) throws NoSuchAlgorithmException {
        StringBuilder sign = new StringBuilder();
        sign.append(method).append(path);
        StringBuilder paramString = new StringBuilder();
        Collections.sort(params);
        for (String parameter : params) {
            if (!parameter.startsWith("sign=")) {
                paramString.append(parameter).append("&");
            }
        }
        if (paramString.length() > 0) {
            sign.append("?").append(paramString.substring(0, paramString.length() - 1)).append(appSecret);
        } else {
            sign.append("?").append(appSecret);
        }
        if (accessToken != null) {
            sign.append(accessToken);
        }

        logger.debug("sign: {}", sign.toString());
        return md5(sign.toString());
    }

    public static String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(input.getBytes(StandardCharsets.UTF_8));
        byte byteData[] = md.digest();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            buffer.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return buffer.toString();
    }

//    public static String truncateAwakenWordIfNeeded(String asrResult, AsrRequestHeader requestHeader) {
//        if(requestHeader.ifTruncateAwakenWord && requestHeader.oneshot && StringUtils.isNotBlank(asrResult) && StringUtils.isNotBlank(requestHeader.awakenWord)) {
//            return CommonUtils.deleteWakeupKey(asrResult, requestHeader.awakenWord);
//        }
//
//        return asrResult;
//    }
//
//    public static String truncateAwakenWordForNlu(String asrResult, AsrRequestHeader requestHeader) {
//        // 如果requestHeader.ifTruncateAwakenWord为true代表之前下发asr识别结果的时候已经截断过唤醒词了，此处不需要重复截断
//        if(!requestHeader.ifTruncateAwakenWord && requestHeader.oneshot && StringUtils.isNotBlank(asrResult) && StringUtils.isNotBlank(requestHeader.awakenWord)) {
//            return CommonUtils.deleteWakeupKey(asrResult, requestHeader.awakenWord);
//        }
//
//        return asrResult;
//    }
}
