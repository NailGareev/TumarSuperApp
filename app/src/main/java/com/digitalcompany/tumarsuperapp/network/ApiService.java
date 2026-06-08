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
import com.digitalcompany.tumarsuperapp.network.models.TransactionHistoryResponse;
import com.digitalcompany.tumarsuperapp.network.models.TopUpRequest;
import com.digitalcompany.tumarsuperapp.network.models.TopUpResponse;
import com.digitalcompany.tumarsuperapp.network.models.PayRequest;
import com.digitalcompany.tumarsuperapp.network.models.PayResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserLookupResponse;
import com.digitalcompany.tumarsuperapp.network.models.CardResponse;
import com.digitalcompany.tumarsuperapp.network.models.TourListResponse;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayRequest;
import com.digitalcompany.tumarsuperapp.network.models.MarketPayResponse;
import com.digitalcompany.tumarsuperapp.network.models.MarketPurchasesResponse;
import com.digitalcompany.tumarsuperapp.network.models.CurrencyRatesResponse;
import com.digitalcompany.tumarsuperapp.network.models.ProfileUpdateRequest;
import com.digitalcompany.tumarsuperapp.network.models.ProfileUpdateResponse;
import com.digitalcompany.tumarsuperapp.network.models.AvatarUploadResponse;
// --- КОНЕЦ: Импорты ---


// Импорты Retrofit аннотаций
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

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

    /**
     * Пополнение баланса текущего пользователя.
     */
    @POST("api/topup")
    Call<TopUpResponse> topUp(@Body TopUpRequest topUpRequest);

    /**
     * Оплата услуг (мобильная связь, ЖКХ, интернет и др.).
     */
    @POST("api/pay")
    Call<PayResponse> pay(@Body PayRequest payRequest);

    /**
     * Поиск клиента Tumar по номеру телефона (для подтверждения получателя перевода).
     */
    @GET("api/lookup-phone")
    Call<UserLookupResponse> lookupUserByPhone(@Query("phone") String phone);

    @GET("api/card")
    Call<CardResponse> getCard();

    @POST("api/card/issue")
    Call<CardResponse> issueCard();

    @GET("api/tours")
    Call<TourListResponse> getTours();

    @GET("api/tours/search")
    Call<TourListResponse> searchTours(
            @Query("destination") String destination,
            @Query("adults") int adults,
            @Query("children") int children
    );

    @POST("api/market/pay")
    Call<MarketPayResponse> marketPay(@Body MarketPayRequest request);

    @GET("api/market/orders")
    Call<MarketPurchasesResponse> getMarketOrders();

    @GET("api/rates")
    Call<CurrencyRatesResponse> getCurrencyRates();

    @PUT("api/profile")
    Call<ProfileUpdateResponse> updateProfile(@Body ProfileUpdateRequest request);

    @Multipart
    @POST("api/profile/avatar")
    Call<AvatarUploadResponse> uploadAvatar(@Part MultipartBody.Part avatar);

    // --- КОНЕЦ: Добавленные методы ---

}