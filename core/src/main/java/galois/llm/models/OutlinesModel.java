package galois.llm.models;

import galois.http.payloads.JSONPayload;
import galois.http.payloads.RegexPayload;
import galois.http.payloads.TextPayload;
import galois.http.services.OutlinesService;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static galois.http.HTTPUtils.executeSyncRequest;
import static galois.utils.Mapper.fromJSON;

public class OutlinesModel implements IModel {
    private final String modelName;
    private final OutlinesService outlinesService;

    public OutlinesModel(String modelName) {
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
        this.outlinesService = retrofit.create(OutlinesService.class);
    }

    @Override
    public String text(String prompt) {
        TextPayload payload = new TextPayload(modelName, prompt);
        Call<String> call = outlinesService.text(payload);
        // TODO: Delete replaceAll?
        return executeSyncRequest(call).replaceAll("\"", "");
    }

    @Override
    public Map<String, Object> json(String prompt, String schema) {
        JSONPayload payload = new JSONPayload(modelName, prompt, schema);
        Call<String> call = outlinesService.json(payload);
        String response = executeSyncRequest(call);
        return fromJSON(response);
    }

    @Override
    public String regex(String prompt, String regex) {
        RegexPayload payload = new RegexPayload(modelName, prompt, regex);
        Call<String> call = outlinesService.regex(payload);
        // TODO: Delete replaceAll?
        return executeSyncRequest(call).replaceAll("\"", "");
    }
}
