package bsf.http;

import bsf.llm.query.exception.LLMQueryException;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

import static bsf.utils.FunctionalUtils.orElseThrow;

@Slf4j
public class HTTPUtils {
    public static <T> T executeSyncRequest(Call<T> call) {
        return orElseThrow(
                () -> {
                    Response<T> response = call.execute();
                    log.debug("Response body is: {}", response.body());
                    return response.body();
                },
                LLMQueryException::new
        );
    }
}
