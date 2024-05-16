package galois.http.services;

import galois.http.payloads.RegexPayload;
import galois.http.payloads.JSONPayload;
import galois.http.payloads.TextPayload;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OutlinesService {
    @POST("outlines/text")
    Call<String> text(@Body TextPayload payload);

    @POST("outlines/regex")
    Call<String> regex(@Body RegexPayload payload);

    @POST("outlines/json")
    Call<String> json(@Body JSONPayload payload);
}
