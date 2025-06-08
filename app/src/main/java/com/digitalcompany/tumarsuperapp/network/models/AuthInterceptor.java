package com.digitalcompany.tumarsuperapp.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    // Ключи SharedPreferences для токена (должны совпадать с LoginActivity)
    private static final String USER_PREFS_NAME = "UserPrefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext(); // Используем application context
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        // Получаем оригинальный запрос
        Request originalRequest = chain.request();

        // Получаем токен из SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(USER_PREFS_NAME, Context.MODE_PRIVATE);
        String token = prefs.getString(KEY_AUTH_TOKEN, null);

        // Строим новый запрос, добавляя заголовок Authorization, если токен есть
        Request.Builder builder = originalRequest.newBuilder();
        if (token != null && !token.isEmpty()) {
            Log.d(TAG, "Adding Authorization header for request to " + originalRequest.url());
            builder.header("Authorization", "Bearer " + token);
        } else {
            Log.w(TAG, "No auth token found for request to " + originalRequest.url());
        }

        Request newRequest = builder.build();

        // Передаем новый запрос дальше по цепочке и возвращаем ответ
        return chain.proceed(newRequest);
    }
}