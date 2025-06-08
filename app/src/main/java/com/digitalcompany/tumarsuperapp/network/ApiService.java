package com.digitalcompany.tumarsuperapp.network;

// Импорты моделей данных
import com.digitalcompany.tumarsuperapp.network.models.LoginRequest;
import com.digitalcompany.tumarsuperapp.network.models.LoginResponse;
import com.digitalcompany.tumarsuperapp.network.models.RegistrationRequest;
import com.digitalcompany.tumarsuperapp.network.models.RegistrationResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
// --- НАЧАЛО: Импорты для перевода и истории ---
import com.digitalcompany.tumarsuperapp.network.models.TransferRequest;
import com.digitalcompany.tumarsuperapp.network.models.TransferResponse;
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse; // Модель для ответа истории
// --- КОНЕЦ: Импорты ---


// Импорты Retrofit аннотаций
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * Интерфейс, описывающий эндпоинты API для Retrofit.
 */
public interface ApiService {

    /**
     * Регистрация нового пользователя.
     */
    @POST("api/register")
    Call<RegistrationResponse> registerUser(@Body RegistrationRequest registrationRequest);

    /**
     * Вход пользователя в систему.
     */
    @POST("api/login")
    Call<LoginResponse> loginUser(@Body LoginRequest loginRequest);

    /**
     * Получение данных профиля текущего пользователя (телефон, баланс).
     */
    @GET("api/profile")
    Call<UserProfileResponse> getUserProfile();

    // --- НАЧАЛО: Добавленные методы для перевода и истории ---

    /**
     * Выполнение перевода средств другому пользователю.
     * Требует валидного JWT токена.
     * @param transferRequest Данные для перевода (номер телефона получателя, сумма).
     * @return Call для выполнения запроса.
     */
    @POST("api/transfer")
    Call<TransferResponse> transferFunds(@Body TransferRequest transferRequest);

    /**
     * Получение истории транзакций для текущего пользователя.
     * Требует валидного JWT токена.
     * @return Call для выполнения запроса, ожидающий объект TransactionHistoryResponse.
     */
    @GET("api/transactions")
    Call<TransactionHistoryResponse> getTransactionHistory();

    // --- КОНЕЦ: Добавленные методы ---


    // Здесь можно добавлять другие методы API по мере необходимости

}