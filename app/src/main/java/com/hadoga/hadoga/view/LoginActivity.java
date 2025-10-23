package com.hadoga.hadoga.view;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;

import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {
    private EditText inputEmail, inputPassword;
    private Button btnLogin;
    private CheckBox checkRemember;
    private HadogaDatabase db;
    private FirebaseFirestore firestore;
    private SharedPreferences preferences;

    // SharedPreferences constantes a usar
    private static final String PREF_NAME = "HadogaPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initUI();
        initListeners();
        loadRememberedUser();
    }

    // Inicializa elementos, preferences y base de datos
    private void initUI() {
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        btnLogin = findViewById(R.id.btnLogin);
        checkRemember = findViewById(R.id.checkRememberMe);

        db = HadogaDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
    }

    // Configura listeners
    private void initListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        TextView textGoRegister = findViewById(R.id.textGoRegister);
        textGoRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    private void attemptLogin() {
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Correo inválido");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 8) {
            inputPassword.setError("La contraseña debe tener al menos 8 caracteres");
            return;
        }

        // Validar conexión antes de intentar con Firebase
        if (!isNetworkAvailable()) {
            // Login local (modo sin conexión)
            Usuario localUser = db.usuarioDao().login(email, password);
            if (localUser != null) {
                showSnackbarLikeToast("Inicio de sesión sin conexión", false);
                onLoginSuccess(localUser);
            } else {
                showSnackbarLikeToast("Credenciales inválidas o usuario no registrado localmente", true);
            }
            return;
        }

        // Si hay conexión: verificar primero local, luego en firestore
        Usuario localUser = db.usuarioDao().login(email, password);
        if (localUser != null) {
            showSnackbarLikeToast("Inicio de sesión con conexión exitoso", false);
            onLoginSuccess(localUser);
            return;
        }

        // Valida en firestore en caso error en base local
        firestore.collection("usuarios").document(email).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String storedPassword = documentSnapshot.getString("contrasena");
                if (storedPassword != null && storedPassword.equals(password)) {
                    showSnackbarLikeToast( "Inicio de sesión desde firebase correcto", false);
                    onLoginSuccess(localUser);
                } else {
                    showSnackbarLikeToast( "Contraseña incorrecta", true);
                }
            } else {
                showSnackbarLikeToast( "Usuario no encontrado", true);
            }
        }).addOnFailureListener(e -> {
            showSnackbarLikeToast("Error al conectar con servidor", true);
        });
    }

    private void onLoginSuccess(Usuario user) {
        if (checkRemember.isChecked()) {
            saveUserCredentials(user.getEmail(), user.getContrasena());
        } else {
            clearUserCredentials();
        }

        showSnackbarLikeToast("Bienvenido, " + user.getNombreClinica(), false);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    // Guarda las credenciales de login en SharedPreferences
    private void saveUserCredentials(String email, String password) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_REMEMBER, true);
        editor.apply();
    }

    // Limpia las credenciales que se han guardado
    private void clearUserCredentials() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    // Carga las credenciales si “Recordarme” está activo
    private void loadRememberedUser() {
        boolean recordarme = preferences.getBoolean(KEY_REMEMBER, false);
        if (recordarme) {
            String savedEmail = preferences.getString(KEY_EMAIL, "");
            String savedPassword = preferences.getString(KEY_PASSWORD, "");
            inputEmail.setText(savedEmail);
            inputPassword.setText(savedPassword);
            checkRemember.setChecked(true);
        }
    }

    private void showSnackbarLikeToast(String message, boolean isError) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        int backgroundColor = isError
                ? ContextCompat.getColor(this, android.R.color.holo_red_dark)
                : ContextCompat.getColor(this, R.color.colorBlue);

        // Crear fondo redondeado
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24f);
        background.setColor(backgroundColor);
        layout.setBackground(background);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 120);
        toast.show();
    }
}