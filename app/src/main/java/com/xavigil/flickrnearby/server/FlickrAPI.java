package com.xavigil.flickrnearby.server;

import com.xavigil.flickrnearby.model.PhotosResponse;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface FlickrAPI {

    @GET("rest/")
    Call<PhotosResponse> getPhotos(@Query("method") String method,
                                   @Query("api_key") String apiKey,
                                   @Query("format") String format,
                                   @Query("nojsoncallback") String nojsoncallback,
                                   @Query("lat") String lat,
                                   @Query("lon")String lon,
                                   @Query("extras") String extras,
                                   @Query("page") String page,
                                   @Query("per_page") String perpage);
}
