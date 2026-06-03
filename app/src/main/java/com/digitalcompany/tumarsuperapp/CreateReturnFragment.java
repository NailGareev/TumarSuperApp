package com.digitalcompany.tumarsuperapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.CreateReturnRequest;
import com.digitalcompany.tumarsuperapp.network.models.ReturnActionResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateReturnFragment extends Fragment {

    private static final String ARG_ORDER_REF = "order_ref";
    private static final String ARG_AMOUNT    = "amount";
    private static final int    MAX_PHOTOS    = 10;
    private static final int    MAX_DIM       = 800;

    private TextInputEditText etOrderRef;
    private TextInputEditText etReason;
    private android.widget.TextView tvPhotoHint;
    private GridLayout        gridPhotos;
    private MaterialButton    btnAddPhotos;
    private MaterialButton    btnSubmit;

    private final List<String> photosBase64 = new ArrayList<>();
    private ApiService apiService;

    private ActivityResultLauncher<String> pickImages;

    public static CreateReturnFragment newInstance(String orderRef, double amount) {
        CreateReturnFragment f = new CreateReturnFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER_REF, orderRef);
        args.putDouble(ARG_AMOUNT, amount);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            apiService = ApiClient.getApiService(getActivity().getApplicationContext());
        }
        pickImages = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris == null || uris.isEmpty()) return;
                    int remaining = MAX_PHOTOS - photosBase64.size();
                    if (remaining <= 0) {
                        Toast.makeText(getContext(), "Максимум 10 фотографий", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<Uri> selected = uris.size() > remaining ? uris.subList(0, remaining) : uris;
                    for (Uri uri : selected) {
                        String b64 = encodeImage(uri);
                        if (b64 != null) photosBase64.add(b64);
                    }
                    updatePhotoGrid();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_create_return, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etOrderRef   = view.findViewById(R.id.et_order_ref);
        etReason     = view.findViewById(R.id.et_reason);
        tvPhotoHint  = view.findViewById(R.id.tv_photo_hint);
        gridPhotos   = view.findViewById(R.id.grid_photos);
        btnAddPhotos = view.findViewById(R.id.btn_add_photos);
        btnSubmit    = view.findViewById(R.id.btn_submit_return);

        Bundle args = getArguments();
        if (args != null) {
            String orderRef = args.getString(ARG_ORDER_REF, "");
            if (!orderRef.isEmpty()) {
                etOrderRef.setText(orderRef);
                etOrderRef.setEnabled(false);
            }
        }

        btnAddPhotos.setOnClickListener(v -> {
            if (photosBase64.size() >= MAX_PHOTOS) {
                Toast.makeText(getContext(), "Максимум 10 фотографий", Toast.LENGTH_SHORT).show();
                return;
            }
            pickImages.launch("image/*");
        });

        btnSubmit.setOnClickListener(v -> submitReturn());
    }

    private void submitReturn() {
        String orderRef = etOrderRef.getText() != null ? etOrderRef.getText().toString().trim() : "";
        String reason   = etReason.getText()   != null ? etReason.getText().toString().trim()   : "";

        if (orderRef.isEmpty()) {
            Toast.makeText(getContext(), "Укажите номер заказа", Toast.LENGTH_SHORT).show();
            return;
        }
        if (reason.isEmpty()) {
            Toast.makeText(getContext(), "Укажите причину возврата", Toast.LENGTH_SHORT).show();
            return;
        }
        if (photosBase64.size() < 2) {
            Toast.makeText(getContext(), "Добавьте минимум 2 фотографии", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Отправка...");

        double amount = getArguments() != null ? getArguments().getDouble(ARG_AMOUNT, 0) : 0;
        CreateReturnRequest request = new CreateReturnRequest(orderRef, amount, reason, new ArrayList<>(photosBase64));

        apiService.createReturn(request).enqueue(new Callback<ReturnActionResponse>() {
            @Override
            public void onResponse(@NonNull Call<ReturnActionResponse> call,
                                   @NonNull Response<ReturnActionResponse> response) {
                if (!isAdded() || getContext() == null) return;
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Отправить заявку");
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(getContext(), "Заявка на возврат создана", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                } else {
                    String msg = (response.body() != null && response.body().message != null)
                            ? response.body().message : "Ошибка отправки заявки";
                    Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ReturnActionResponse> call, @NonNull Throwable t) {
                if (!isAdded() || getContext() == null) return;
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Отправить заявку");
                Toast.makeText(getContext(), "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePhotoGrid() {
        if (!isAdded() || getContext() == null) return;
        gridPhotos.removeAllViews();
        int dp80 = (int) (80 * getResources().getDisplayMetrics().density);
        int dp4  = (int) (4  * getResources().getDisplayMetrics().density);
        for (String b64 : photosBase64) {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            ImageView iv = new ImageView(getContext());
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width  = dp80;
            lp.height = dp80;
            lp.setMargins(dp4, dp4, dp4, dp4);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(bmp);
            iv.setBackgroundResource(R.drawable.bg_chip_purple);
            gridPhotos.addView(iv);
        }
        tvPhotoHint.setText("Добавьте от 2 до 10 фотографий (" + photosBase64.size() + " выбрано)");
        btnAddPhotos.setEnabled(photosBase64.size() < MAX_PHOTOS);
    }

    @Nullable
    private String encodeImage(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            Bitmap original = BitmapFactory.decodeStream(is);
            if (original == null) return null;
            int w = original.getWidth();
            int h = original.getHeight();
            float scale = Math.min((float) MAX_DIM / w, (float) MAX_DIM / h);
            if (scale < 1f) {
                original = Bitmap.createScaledBitmap(original, (int)(w * scale), (int)(h * scale), true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            original.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }
}
