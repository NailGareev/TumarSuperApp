package com.digitalcompany.tumarsuperapp.network;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {

    // !!! ВАЖНО: Убедитесь, что этот адрес доступен с вашего устройства/эмулятора !!!
    // Эмулятор: http://10.0.2.2:3000/ (если сервер на localhost:3000)
    // Физическое устройство: http://<IP-адрес вашего компьютера в сети>:3000/
    private static final String BASE_URL = "http://10.0.2.2:3000/";

    private static Retrofit retrofit = null;
    private static OkHttpClient okHttpClient = null; // Кэшируем OkHttpClient

    // Метод для получения клиента Retrofit
    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            // Создаем OkHttpClient один раз (ленивая инициализация)
            if (okHttpClient == null) {
                // Настройка логгирования HTTP запросов
                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                // Уровни: NONE, BASIC, HEADERS, BODY
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY); // Логируем тело запроса/ответа

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

            // Создаем Retrofit клиент
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL) // Базовый URL вашего API
                    .client(okHttpClient) // Используем настроенный OkHttpClient
                    .addConverterFactory(GsonConverterFactory.create()) // Используем Gson для конвертации JSON
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