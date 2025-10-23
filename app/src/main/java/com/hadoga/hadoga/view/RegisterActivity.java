package com.hadoga.hadoga.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;

public class RegisterActivity extends AppCompatActivity {
    // Definición de elementos
    private EditText inputNombreClinica, inputEmail, inputPassword, inputConfirmPassword;
    private Button btnRegister;
    private HadogaDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initUI();
        initListeners();
    }

    // Inicializa las vistas y base de datos
    private void initUI() {
        inputNombreClinica = findViewById(R.id.inputClinicName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        btnRegister = findViewById(R.id.btnCreateAccount);

        // Inicializar Room
        db = HadogaDatabase.getInstance(this);
    }

    // Configura listeners de botones
    private void initListeners() {
        // Botón de regresar
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // Botón de registrar
        btnRegister.setOnClickListener(v -> registerUser());

        // Texto para volver al login
        TextView textGoLogin = findViewById(R.id.textGoLogin);
        textGoLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Valida los datos ingresados y registra el usuario
    private void registerUser() {
        String nombre = inputNombreClinica.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();

        // 🔹 Validaciones
        if (TextUtils.isEmpty(nombre) || nombre.length() < 3) {
            inputNombreClinica.setError("Debe tener al menos 3 caracteres");
            return;
        }

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Correo inválido");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            inputPassword.setError("Debe tener al menos 6 caracteres");
            return;
        }

        if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Las contraseñas no coinciden");
            return;
        }

        // 🔹 Validar si ya existe el email
        if (db.usuarioDao().login(email, password) != null) {
            inputEmail.setError("Este correo ya está registrado");
            return;
        }

        // 🔹 Insertar usuario
        Usuario nuevo = new Usuario(nombre, email, password);
        db.usuarioDao().insert(nuevo);

        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();

        // 🔹 Volver al login
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}