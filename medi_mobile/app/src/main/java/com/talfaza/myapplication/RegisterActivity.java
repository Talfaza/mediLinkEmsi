package com.talfaza.myapplication;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.app.AlertDialog;
import android.widget.Toast;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.POST;
import android.util.Log;
import android.view.View;
import android.content.Intent;

public class RegisterActivity extends AppCompatActivity {
    private TextInputLayout firstNameLayout;
    private TextInputLayout lastNameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout birthdayLayout;
    private TextInputEditText firstNameInput;
    private TextInputEditText lastNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText birthdayInput;
    private MaterialButton registerButton;
    private TextView loginPrompt;
    private TextView signInText;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormatter;
    private TextView successMessage;

    // Retrofit API classes
    public static class PatientRegisterRequest {
        public String firstName, lastName, email, password, birthday;
    }
    public static class PatientRegisterResponse {
        public boolean success;
        public String message;
    }
    public interface ApiService {
        @POST("/medilink/api/patient/register")
        Call<PatientRegisterResponse> registerPatient(@Body PatientRegisterRequest request);
    }
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize date formatter
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = Calendar.getInstance();

        // Initialize views
        firstNameLayout = findViewById(R.id.firstNameLayout);
        lastNameLayout = findViewById(R.id.lastNameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        birthdayLayout = findViewById(R.id.birthdayLayout);
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        birthdayInput = findViewById(R.id.birthdayInput);
        registerButton = findViewById(R.id.registerButton);
        loginPrompt = findViewById(R.id.loginPrompt);
        signInText = findViewById(R.id.signInText);
        successMessage = findViewById(R.id.successMessage);

        // Set up click listeners
        registerButton.setOnClickListener(v -> handleRegister());
        loginPrompt.setOnClickListener(v -> finish());
        signInText.setOnClickListener(v -> finish());
        birthdayInput.setOnClickListener(v -> showDatePicker());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.1.28:8080")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDate.set(year, month, dayOfMonth);
                birthdayInput.setText(dateFormatter.format(selectedDate.getTime()));
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void handleRegister() {
        // Clear previous errors
        firstNameLayout.setError(null);
        lastNameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        birthdayLayout.setError(null);

        // Get input values
        String firstName = firstNameInput.getText().toString().trim();
        String lastName = lastNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String birthday = birthdayInput.getText().toString().trim();

        // Validate inputs
        boolean isValid = true;

        if (firstName.isEmpty()) {
            firstNameLayout.setError("First name is required");
            isValid = false;
        }

        if (lastName.isEmpty()) {
            lastNameLayout.setError("Last name is required");
            isValid = false;
        }

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
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }

        if (birthday.isEmpty()) {
            birthdayLayout.setError("Birthday is required");
            isValid = false;
        }

        if (isValid) {
            registerButton.setText("Creating Account...");
            registerButton.setEnabled(false);
            successMessage.setText("");
            // Prepare request
            PatientRegisterRequest req = new PatientRegisterRequest();
            req.firstName = firstName;
            req.lastName = lastName;
            req.email = email;
            req.password = password;
            req.birthday = selectedDate != null ? dateFormatter.format(selectedDate.getTime()) : birthday;
            apiService.registerPatient(req).enqueue(new Callback<PatientRegisterResponse>() {
                @Override
                public void onResponse(Call<PatientRegisterResponse> call, Response<PatientRegisterResponse> response) {
                    registerButton.setText("Create Account");
                    registerButton.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        PatientRegisterResponse res = response.body();
                        if (res.success) {
                            successMessage.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            successMessage.setText(res.message);
                            successMessage.setVisibility(View.VISIBLE);
                            // Redirect to MainActivity after 2 seconds
                            new android.os.Handler().postDelayed(() -> {
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            }, 2000);
                        } else {
                            successMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                            successMessage.setText(res.message);
                            successMessage.setVisibility(View.VISIBLE);
                        }
                    } else {
                        String errorMsg = "Registration failed. Please try again.";
                        try {
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e("RegisterActivity", "Error body: " + errorBody);
                                errorMsg = "Registration failed: " + errorBody;
                            }
                        } catch (Exception e) {
                            Log.e("RegisterActivity", "Error reading errorBody", e);
                        }
                        successMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                        successMessage.setText(errorMsg);
                        successMessage.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onFailure(Call<PatientRegisterResponse> call, Throwable t) {
                    registerButton.setText("Create Account");
                    registerButton.setEnabled(true);
                    Log.e("RegisterActivity", "Network error", t);
                    successMessage.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    successMessage.setText("Network error: " + t.getMessage());
                    successMessage.setVisibility(View.VISIBLE);
                }
            });
        }
    }
} 