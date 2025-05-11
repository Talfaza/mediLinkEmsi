package com.talfaza.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.view.View;
import android.widget.Toast;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerPrompt;
    private TextView createAccountText;
    private TextView loginErrorMessage;
    private ApiService apiService;
    private OkHttpClient okHttpClient;

    public static class LoginRequest {
        public String email, password;
    }

    public static class LoginResponse {
        public boolean success;
        public String message;
        public String redirect;
    }

    public interface ApiService {
        @POST("/medilink/api/patient/login")
        Call<LoginResponse> login(@Body LoginRequest request);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerPrompt = findViewById(R.id.registerPrompt);
        createAccountText = findViewById(R.id.createAccountText);
        loginErrorMessage = findViewById(R.id.loginErrorMessage);

        // Set up click listeners
        loginButton.setOnClickListener(v -> handleLogin());
        registerPrompt.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
        createAccountText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Set up OkHttpClient with cookie handling
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .cookieJar(new PersistentCookieJar(this))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.28:8080")
            .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    private void handleLogin() {
        // Clear previous errors
        emailLayout.setError(null);
        passwordLayout.setError(null);

        // Get input values
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        boolean isValid = true;

        if (email.isEmpty()) {
            emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email format");
            isValid = false;
        }

        if (password.isEmpty()) {
            passwordLayout.setError("Password is required");
            isValid = false;
        }

        loginErrorMessage.setText("");
        if (isValid) {
            loginButton.setText("Logging in...");
            loginButton.setEnabled(false);
            LoginRequest req = new LoginRequest();
            req.email = email;
            req.password = password;
            apiService.login(req).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    loginButton.setText("Sign In");
                    loginButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse res = response.body();
                        if (res.success) {
                            startActivity(new Intent(MainActivity.this, DashboardActivity.class));
                            finish();
                        } else {
                            loginErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            loginErrorMessage.setText(res.message);
                            loginErrorMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        loginErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        loginErrorMessage.setText("Login failed. Please try again.");
                        loginErrorMessage.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    loginButton.setText("Sign In");
                    loginButton.setEnabled(true);
                    loginErrorMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    loginErrorMessage.setText("Network error: " + t.getMessage());
                    loginErrorMessage.setVisibility(View.VISIBLE);
                }
            });
        }
    }
}