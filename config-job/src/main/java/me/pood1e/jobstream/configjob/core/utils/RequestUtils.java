package me.pood1e.jobstream.configjob.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import me.pood1e.jobstream.common.ClusterInfo;
import me.pood1e.jobstream.common.Function;
import me.pood1e.jobstream.common.Job;
import me.pood1e.jobstream.common.Plugin;
import me.pood1e.jobstream.configjob.core.api.ConfigApi;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.List;

@UtilityClass
public class RequestUtils {

    private static String apiServer;

    private static ConfigApi api;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static <T> T convertResponse(Call<ResponseBody> call, TypeReference<T> reference) {
        String response = null;
        try (ResponseBody responseBody = call.execute().body()) {
            if (responseBody == null) {
                throw new RuntimeException();
            }
            response = responseBody.string();
            return MAPPER.readValue(response, reference);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkApiServerNotNull() {
        if (apiServer == null) {
            throw new RuntimeException("Api server not set");
        }
    }

    public static <T> Function<T> getFunctionConfig(String id) {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getFunction(id);
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public static List<Function<?>> getFunctionConfigs(String id) {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getFunctionsByJob(id);
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public static Job getJob(String id) {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getJob(id);
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public static List<Plugin> getJobPlugins(String id) {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getJobJars(id);
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public synchronized static void setApiServer(String apiServer) {
        if (RequestUtils.apiServer == null) {
            RequestUtils.apiServer = apiServer;
            api = new Retrofit.Builder().baseUrl(apiServer)
                    .build().create(ConfigApi.class);
        }
    }

    public static ClusterInfo getClusterInfo() {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getClusterInfo();
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public static Plugin getPluginByClassName(String className) {
        checkApiServerNotNull();
        Call<ResponseBody> call = api.getPluginByClass(className);
        return convertResponse(call, new TypeReference<>() {
        });
    }

    public static String getPluginJarUrl(String id) {
        checkApiServerNotNull();
        return apiServer + "/plugin/" + id + "/jar";
    }
}
