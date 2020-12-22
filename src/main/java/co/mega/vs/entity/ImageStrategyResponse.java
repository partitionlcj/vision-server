package co.mega.vs.entity;

import com.google.gson.annotations.SerializedName;

public class ImageStrategyResponse {

    @SerializedName("request_id")
    public String requestId;

    @SerializedName("result_code")
    public String resultCode;

    public Data data;

    public class Data {

        @SerializedName("is_directed")
        public boolean isDirected;
    }
}
