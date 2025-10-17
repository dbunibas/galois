package bsf.http.services;

import bsf.http.payloads.TextPayload;
import bsf.http.payloads.JSONPayload;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LlamaCppService {
    @POST("llamacpp/text")
    Call<String> text(@Body TextPayload payload);

    @POST("llamacpp/json")
    Call<String> json(@Body JSONPayload payload);
}
