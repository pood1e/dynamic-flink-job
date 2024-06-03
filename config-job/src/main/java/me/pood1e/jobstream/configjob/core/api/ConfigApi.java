package me.pood1e.jobstream.configjob.core.api;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ConfigApi {
    @GET("/function/{id}")
    Call<ResponseBody> getFunction(@Path("id") String id);

    @GET("/job/{id}/functions")
    Call<ResponseBody> getFunctionsByJob(@Path("id") String id);

    @GET("/job/{id}")
    Call<ResponseBody> getJob(@Path("id") String id);

    @GET("/job/{id}/plugins")
    Call<ResponseBody> getJobJars(@Path("id") String id);

    @GET("/cluster/info")
    Call<ResponseBody> getClusterInfo();

    @GET("/plugin/class")
    Call<ResponseBody> getPluginByClass(@Query("className") String className);
}
