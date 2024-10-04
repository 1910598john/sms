package com.example.smsserver;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
public interface MyApi {
    @POST("api/smslogin/")
    Call<ResponseBody> loginUser(@Body LoginRequest loginRequest);

    @POST("api/getsms/")
    Call<ResponseBody> getSms(@Body Sms sms);

    @POST("api/deletesms/")
    Call<ResponseBody> deleteSMS(@Body DeleteSMS deleteSMS);

}

