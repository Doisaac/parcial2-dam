package com.hadoga.hadoga.view;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {
    private EditText inputFullName, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister;
    private HadogaDatabase db;
    private FirebaseFirestore firestore;

    // Regex
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI();
        initListeners();
    }

    // Inicializa las vistas y base de datos
    private void initUI() {
        inputFullName = findViewById(R.id.inputFullName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnCreateAccount);

        db = HadogaDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();
    }

    // Configura listeners de botones
    private void initListeners() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        btnRegister.setOnClickListener(v -> registerUser());

        TextView textGoLogin = findViewById(R.id.textGoLogin);
        textGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } else {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    // Valida los datos ingresados y registra el usuario
    private void registerUser() {
        String fullName = inputFullName.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // Validaciones
        if (TextUtils.isEmpty(fullName) || fullName.length() < 7) {
            inputFullName.setError("Debe tener al menos 7 caracteres");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("El correo es inválido");
            return;
        }

        if (TextUtils.isEmpty(password) || !PASSWORD_PATTERN.matcher(password).matches()) {
            inputPassword.setError("Debe tener mínimo 8 caracteres y también ser alfanumérica");
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }

        // Validar si ya existe en la BD local
        Usuario existente = null;
        for (Usuario u : db.usuarioDao().getAllUsuarios()) {
            if (u.getEmail().equalsIgnoreCase(email)) {
                existente = u;
                break;
            }
        }
        if (existente != null) {
            inputEmail.setError("Este correo ya está registrado");
            return;
        }

        // Crear nuevo usuario
        Usuario nuevo = new Usuario(fullName, email, password, "PENDIENTE");

        // Datos para Firestore
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("nombre", fullName);
        userMap.put("email", email);
        userMap.put("contrasena", password);
        userMap.put("sync_status", "SINCRONIZADO");

        // Verificar conexión
        if (!isNetworkAvailable()) {
            // Guardar localmente si no hay red
            new Thread(() -> db.usuarioDao().insert(nuevo)).start();

            showSnackbarLikeToast("Usuario guardado localmente, pendiente de sincronizar a la nube");

            goToLogin();
            return;
        }

        // Si hay red, intenta subir a Firestore
        firestore.collection("usuarios")
                .document(email)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    new Thread(() -> {
                        nuevo.setSyncStatus("SINCRONIZADO");
                        db.usuarioDao().insert(nuevo);
                    }).start();
                    showSnackbarLikeToast("Usuario registrado correctamente");
                    goToLogin();
                })
                .addOnFailureListener(e -> {
                    new Thread(() -> db.usuarioDao().insert(nuevo)).start();
                    showSnackbarLikeToast("Usuario guardado localmente, pendiente de sincronizar a la nube");
                    goToLogin();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showSnackbarLikeToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast, null);

        TextView text = layout.findViewById(R.id.toast_message);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 120);
        toast.show();
    }

}