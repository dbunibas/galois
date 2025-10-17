package bsf.llm.models;

import bsf.http.payloads.JSONPayload;
import bsf.http.payloads.TextPayload;
import bsf.http.services.LlamaCppService;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static bsf.http.HTTPUtils.executeSyncRequest;
import static bsf.utils.Mapper.fromJsonToMap;

public class LlamaCppModel implements IModel {
    private final String modelName;
    private final LlamaCppService llamaCppService;

    public LlamaCppModel(String modelName) {
        this.modelName = modelName;

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://127.0.0.1:8000/")
                .client(client)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
        llamaCppService = retrofit.create(LlamaCppService.class);
    }

    @Override
    public String text(String prompt) {
        TextPayload payload = new TextPayload(modelName, prompt);
        Call<String> call = llamaCppService.text(payload);
        // TODO: Delete replaceAll?
        return executeSyncRequest(call).replaceAll("\"", "");
    }

    @Override
    public Map<String, Object> json(String prompt, String schema) {
        JSONPayload payload = new JSONPayload(modelName, prompt, schema);
        Call<String> call = llamaCppService.json(payload);
        String response = executeSyncRequest(call);
        return fromJsonToMap(response);
    }
}
