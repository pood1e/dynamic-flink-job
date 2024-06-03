package me.pood1e.jobstream.plugintest2;

import lombok.extern.slf4j.Slf4j;
import me.pood1e.jobstream.pluginbase.CfgSource;
import me.pood1e.jobstream.pluginbase.MarkSourceType;
import me.pood1e.jobstream.pluginbase.SourceThreadType;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

@Slf4j
@MarkSourceType(SourceThreadType.MULTI_PERIOD)
public class ApiSource implements CfgSource<ApiResponse, ApiConfig, ApiFetchConfig> {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public ApiResponse fetch(ApiFetchConfig config) {
        Headers headers = Headers.of(config.getHeaders() == null? new HashMap<>() : config.getHeaders());
        RequestBody body = config.getBody() != null ? RequestBody.create(config.getBody().getBytes(StandardCharsets.UTF_8)) : null;
        Request request = new Request.Builder().url(config.getUrl())
                .headers(headers)
                .method(config.getMethod(), body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return ApiResponse.builder()
                    .headers(response.headers().toMultimap())
                    .body(response.body() != null ? response.body().string() : null)
                    .statusCode(response.code())
                    .build();
        } catch (IOException e) {
            log.warn("Failed to fetch api response", e);
        }
        return null;
    }

    @Override
    public List<ApiFetchConfig> getFetchConfigs(ApiConfig apiConfig) {
        return apiConfig.getFetchConfigs();
    }
}
