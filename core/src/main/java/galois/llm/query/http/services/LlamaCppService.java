package galois.llm.query.http.services;

import galois.llm.query.http.payloads.JSONPayload;
import galois.llm.query.http.payloads.TextPayload;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface LlamaCppService {
    @POST("llamacpp/text")
    Call<String> text(@Body TextPayload payload);

    @POST("llamacpp/json")
    Call<String> json(@Body JSONPayload payload);
}
