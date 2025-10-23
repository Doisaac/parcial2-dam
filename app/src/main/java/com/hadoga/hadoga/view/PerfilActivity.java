package com.hadoga.hadoga.view;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.model.database.HadogaDatabase;
import com.hadoga.hadoga.model.entities.Usuario;

public class PerfilActivity extends AppCompatActivity {

    private EditText inputNombre;
    private TextView textNombreActual;
    private Button btnGuardar, btnRegresar;
    private HadogaDatabase db;
    private FirebaseFirestore firestore;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        inputNombre = findViewById(R.id.inputNombre);
        textNombreActual = findViewById(R.id.textNombreActual);
        btnGuardar = findViewById(R.id.btnGuardarNombre);
        btnRegresar = findViewById(R.id.btnRegresar);

        db = HadogaDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        // Recuperar el usuario actual por su email
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            usuario = db.usuarioDao().findByEmail(email);
        }

        // Mostrar el nombre actual
        if (usuario != null) {
            textNombreActual.setText("Nombre actual: " + usuario.getNombreClinica());
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void guardarCambios() {
        String nuevoNombre = inputNombre.getText().toString().trim();

        if (TextUtils.isEmpty(nuevoNombre) || nuevoNombre.length() < 3) {
            inputNombre.setError("Debe tener al menos 3 caracteres");
            return;
        }

        if (usuario == null) {
            Toast.makeText(this, "Usuario no encontrado en la base local", Toast.LENGTH_SHORT).show();
            return;
        }

        //  Actualizar localmente en Room
        usuario.setNombreClinica(nuevoNombre);
        new Thread(() -> db.usuarioDao().update(usuario)).start();

        //  Actualizar en Firestore (en la nube)
        firestore.collection("usuarios")
                .document(usuario.getEmail())
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    textNombreActual.setText("Nombre actual: " + nuevoNombre);
                    inputNombre.setText("");
                    Toast.makeText(this, "Nombre actualizado (local + nube)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al sincronizar con la nube", Toast.LENGTH_SHORT).show());
    }
}
