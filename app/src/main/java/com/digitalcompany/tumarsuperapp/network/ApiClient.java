package com.digitalcompany.tumarsuperapp.network;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    public static final String BASE_URL = "http://193.108.113.91:3000/";

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null; // Кэшируем OkHttpClient

    // Метод для получения клиента Retrofit
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Создаем OkHttpClient один раз (ленивая инициализация)
            if (okHttpClient == null) {
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(android.util.Log.isLoggable("ApiClient", android.util.Log.DEBUG)
                        ? HttpLoggingInterceptor.Level.BASIC
                        : HttpLoggingInterceptor.Level.NONE);

                // Создаем строитель OkHttpClient
                OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

                // Добавляем Interceptor для логирования
                httpClientBuilder.addInterceptor(loggingInterceptor);

                // Добавляем Interceptor для автоматической подстановки токена авторизации
                httpClientBuilder.addInterceptor(new com.digitalcompany.tumarsuperapp.network.AuthInterceptor(context.getApplicationContext()));

                // Устанавливаем таймауты соединения
                httpClientBuilder.connectTimeout(30, TimeUnit.SECONDS);
                httpClientBuilder.readTimeout(30, TimeUnit.SECONDS);
                httpClientBuilder.writeTimeout(30, TimeUnit.SECONDS);

                // Собираем OkHttpClient
                okHttpClient = httpClientBuilder.build();
            }

            // Gson with ISO 8601 date parsing (mysql2 serializes timestamps as "2025-01-17T07:23:45.000Z")
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, (JsonDeserializer<Date>) (json, type, ctx) -> {
                        String s = json.getAsString();
                        String[] formats = {
                            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                            "yyyy-MM-dd'T'HH:mm:ssZ"
                        };
                        for (String fmt : formats) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                return sdf.parse(s);
                            } catch (Exception ignored) {}
                        }
                        Log.w("ApiClient", "Could not parse date: " + s);
                        return null;
                    })
                    .create();

            // Создаем Retrofit клиент
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }

    // Упрощенный метод для получения готового ApiService
    public static ApiService getApiService(Context context) {
        // Получаем Retrofit клиент (создастся, если еще не создан)
        // и создаем реализацию интерфейса ApiService
        return getClient(context.getApplicationContext()).create(ApiService.class);
    }

    // Метод для сброса клиента (может понадобиться при смене настроек или выходе)
    public static void resetClient() {
        retrofit = null;
        okHttpClient = null; // Сбрасываем и OkHttpClient
    }
}