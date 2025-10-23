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
    private TextView textNombreActual, textEditarTitulo;
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
        textEditarTitulo = findViewById(R.id.textEditarTitulo);
        btnGuardar = findViewById(R.id.btnGuardarNombre);
        btnRegresar = findViewById(R.id.btnRegresar);

        db = HadogaDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            usuario = db.usuarioDao().findByEmail(email);
        }

        if (usuario != null) {
            textNombreActual.setText("Nombre actual: " + usuario.getNombreClinica());
        }

        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnRegresar.setOnClickListener(v -> finish());
    }

    private void guardarCambios() {
        String nuevoNombre = inputNombre.getText().toString().trim();

        if (TextUtils.isEmpty(nuevoNombre) || nuevoNombre.length() < 7) {
            inputNombre.setError("Debe tener al menos 7 caracteres");
            return;
        }

        usuario.setNombreClinica(nuevoNombre);
        new Thread(() -> db.usuarioDao().update(usuario)).start();

        firestore.collection("usuarios")
                .document(usuario.getEmail())
                .update("nombre", nuevoNombre)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Nombre actualizado correctamente", Toast.LENGTH_SHORT).show();
                    textNombreActual.setText("Nombre actual: " + nuevoNombre);
                    inputNombre.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al sincronizar con la nube", Toast.LENGTH_SHORT).show());
    }
}
