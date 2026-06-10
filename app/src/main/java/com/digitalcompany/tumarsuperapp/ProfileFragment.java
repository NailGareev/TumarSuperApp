package com.digitalcompany.tumarsuperapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.digitalcompany.tumarsuperapp.network.ApiClient;
import com.digitalcompany.tumarsuperapp.network.ApiService;
import com.digitalcompany.tumarsuperapp.network.models.AvatarUploadResponse;
import com.digitalcompany.tumarsuperapp.network.models.ProfileUpdateRequest;
import com.digitalcompany.tumarsuperapp.network.models.ProfileUpdateResponse;
import com.digitalcompany.tumarsuperapp.network.models.UserProfileResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_PROFILE_FIRST = "profile_first_name";
    private static final String KEY_PROFILE_LAST  = "profile_last_name";
    private static final String KEY_PROFILE_EMAIL = "profile_email";
    private static final String KEY_PROFILE_PHONE = "profile_phone";
    private static final String KEY_PROFILE_AVATAR = "profile_avatar_url";
    private static final String KEY_PROFILE_OPS   = "profile_op_count";

    private ImageView imgAvatar;
    private TextView textName, textPhone, textMenuName, textMenuPhone, textMenuEmail;
    private TextView textStatCashback, textStatOperations, textStatLevel;
    private TextView textVerifiedBadge;

    private ApiService apiService;
    private SharedPreferences prefs;

    private ActivityResultLauncher<Intent> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadAvatar(imageUri);
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireActivity().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        apiService = ApiClient.getApiService(requireActivity().getApplicationContext());

        imgAvatar         = view.findViewById(R.id.img_avatar);
        textName          = view.findViewById(R.id.text_profile_name);
        textPhone         = view.findViewById(R.id.text_profile_phone);
        textMenuName      = view.findViewById(R.id.text_menu_name);
        textMenuPhone     = view.findViewById(R.id.text_menu_phone);
        textMenuEmail     = view.findViewById(R.id.text_menu_email);
        textStatCashback   = view.findViewById(R.id.text_stat_cashback);
        textStatOperations = view.findViewById(R.id.text_stat_operations);
        textStatLevel      = view.findViewById(R.id.text_stat_level);
        textVerifiedBadge  = view.findViewById(R.id.text_verified_badge);

        // Restore cached data instantly
        loadFromCache();

        // Then refresh from API
        fetchProfile();

        view.findViewById(R.id.btn_edit_profile).setOnClickListener(v -> showEditBottomSheet());
        View profileBell = view.findViewById(R.id.btn_profile_bell);
        if (profileBell != null) {
            profileBell.setOnClickListener(v -> {
                if (getActivity() == null) return;
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                                R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.fragment_container, new NotificationsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }
        view.findViewById(R.id.btn_change_avatar).setOnClickListener(v -> pickImage());
        view.findViewById(R.id.row_name).setOnClickListener(v -> showEditBottomSheet());

        view.findViewById(R.id.action_change_pin).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PinSetupActivity.class);
            intent.putExtra("IS_CHANGE_MODE", true);
            startActivity(intent);
        });

        view.findViewById(R.id.row_biometrics).setOnClickListener(v -> {
            if (getActivity() == null) return;
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out,
                            R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.fragment_container, new BiometricSettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        LinearLayout btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            if (getActivity() != null) {
                LoginActivity.logout(requireActivity());
            }
        });

        return view;
    }

    private void loadFromCache() {
        String first = prefs.getString(KEY_PROFILE_FIRST, "");
        String last  = prefs.getString(KEY_PROFILE_LAST, "");
        String phone = prefs.getString(KEY_PROFILE_PHONE, "");
        String email = prefs.getString(KEY_PROFILE_EMAIL, "");
        String avatar = prefs.getString(KEY_PROFILE_AVATAR, null);
        int ops = prefs.getInt(KEY_PROFILE_OPS, 0);

        if (!first.isEmpty() || !last.isEmpty()) {
            String fullName = (first + " " + last).trim();
            textName.setText(fullName);
            textMenuName.setText(fullName);
        }
        if (!phone.isEmpty()) {
            textPhone.setText(phone);
            textMenuPhone.setText(phone);
        }
        if (!email.isEmpty()) {
            textMenuEmail.setText(email);
        }
        textStatOperations.setText(String.valueOf(ops));
        textStatCashback.setText("₸ 0");
        textStatLevel.setText("Бронза");

        loadAvatar(avatar);
    }

    private void loadAvatar(@Nullable String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.isEmpty() && getContext() != null) {
            String fullUrl = ApiClient.BASE_URL.replaceAll("/$", "") + avatarUrl;
            Glide.with(requireContext())
                    .load(fullUrl)
                    .circleCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .placeholder(R.drawable.ic_person_black_24dp)
                    .into(imgAvatar);
            imgAvatar.setColorFilter(null);
        }
    }

    private void fetchProfile() {
        apiService.getUserProfile().enqueue(new Callback<UserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<UserProfileResponse> call,
                                   @NonNull Response<UserProfileResponse> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfileResponse p = response.body();
                    String first = p.getFirstName() != null ? p.getFirstName() : "";
                    String last  = p.getLastName()  != null ? p.getLastName()  : "";
                    String phone = p.getPhone()      != null ? p.getPhone()     : "";
                    String email = p.getEmail()      != null ? p.getEmail()     : "";
                    String avatar = p.getAvatarUrl();
                    int ops = p.getOperationCount();

                    String fullName = (first + " " + last).trim();
                    textName.setText(fullName);
                    textMenuName.setText(fullName);
                    textPhone.setText(phone);
                    textMenuPhone.setText(phone);
                    textMenuEmail.setText(email);
                    textStatOperations.setText(String.valueOf(ops));

                    prefs.edit()
                            .putString(KEY_PROFILE_FIRST, first)
                            .putString(KEY_PROFILE_LAST, last)
                            .putString(KEY_PROFILE_PHONE, phone)
                            .putString(KEY_PROFILE_EMAIL, email)
                            .putString(KEY_PROFILE_AVATAR, avatar != null ? avatar : "")
                            .putInt(KEY_PROFILE_OPS, ops)
                            .apply();

                    loadAvatar(avatar);
                    updateVerifiedBadge(!first.isEmpty() && !phone.isEmpty());
                }
            }

            @Override
            public void onFailure(@NonNull Call<UserProfileResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Profile fetch failed", t);
            }
        });
    }

    private void showEditBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_edit_profile, null);
        dialog.setContentView(sheet);

        TextInputEditText editFirst = sheet.findViewById(R.id.edit_first_name);
        TextInputEditText editLast  = sheet.findViewById(R.id.edit_last_name);
        TextInputEditText editEmail = sheet.findViewById(R.id.edit_email);

        editFirst.setText(prefs.getString(KEY_PROFILE_FIRST, ""));
        editLast.setText(prefs.getString(KEY_PROFILE_LAST, ""));
        editEmail.setText(prefs.getString(KEY_PROFILE_EMAIL, ""));

        sheet.findViewById(R.id.btn_change_photo).setOnClickListener(v -> {
            dialog.dismiss();
            pickImage();
        });

        sheet.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        sheet.findViewById(R.id.btn_save).setOnClickListener(v -> {
            String fn = editFirst.getText() != null ? editFirst.getText().toString().trim() : "";
            String ln = editLast.getText()  != null ? editLast.getText().toString().trim()  : "";
            String em = editEmail.getText() != null ? editEmail.getText().toString().trim() : "";

            if (fn.isEmpty() || ln.isEmpty() || em.isEmpty()) {
                Toast.makeText(getContext(), "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            apiService.updateProfile(new ProfileUpdateRequest(fn, ln, em))
                    .enqueue(new Callback<ProfileUpdateResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ProfileUpdateResponse> call,
                                               @NonNull Response<ProfileUpdateResponse> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful() && response.body() != null
                                    && response.body().isSuccess()) {
                                prefs.edit()
                                        .putString(KEY_PROFILE_FIRST, fn)
                                        .putString(KEY_PROFILE_LAST, ln)
                                        .putString(KEY_PROFILE_EMAIL, em)
                                        .apply();
                                String fullName = (fn + " " + ln).trim();
                                textName.setText(fullName);
                                textMenuName.setText(fullName);
                                textMenuEmail.setText(em);
                                dialog.dismiss();
                                Toast.makeText(getContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Ошибка обновления", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ProfileUpdateResponse> call,
                                              @NonNull Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Нет соединения", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void uploadAvatar(Uri imageUri) {
        try {
            File file = uriToFile(imageUri);
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part part = MultipartBody.Part.createFormData("avatar", file.getName(), requestBody);

            apiService.uploadAvatar(part).enqueue(new Callback<AvatarUploadResponse>() {
                @Override
                public void onResponse(@NonNull Call<AvatarUploadResponse> call,
                                       @NonNull Response<AvatarUploadResponse> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null
                            && response.body().isSuccess()) {
                        String url = response.body().getAvatarUrl();
                        prefs.edit().putString(KEY_PROFILE_AVATAR, url).apply();
                        loadAvatar(url);
                        Toast.makeText(getContext(), "Фото обновлено", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<AvatarUploadResponse> call, @NonNull Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Нет соединения", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Failed to prepare avatar file", e);
            Toast.makeText(getContext(), "Не удалось обработать изображение", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateVerifiedBadge(boolean verified) {
        if (textVerifiedBadge == null) return;
        if (verified) {
            textVerifiedBadge.setText("✓ Верифицирован");
            textVerifiedBadge.setTextColor(0xFF1A8A4A);
            textVerifiedBadge.setBackground(
                    androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_verified));
        } else {
            textVerifiedBadge.setText("✗ Не верифицирован");
            textVerifiedBadge.setTextColor(0xFFC62828);
            textVerifiedBadge.setBackground(
                    androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_unverified));
        }
    }

    private File uriToFile(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Cannot open stream for URI");
        File tempFile = File.createTempFile("avatar_", ".jpg", requireContext().getCacheDir());
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buf = new byte[4096];
            int len;
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
        inputStream.close();
        return tempFile;
    }
}
