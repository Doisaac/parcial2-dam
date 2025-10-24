package com.hadoga.hadoga.view;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import java.util.List;

public class PerfilActivity extends AppCompatActivity {

    private EditText inputNombre;
    private TextView textNombreActual, textEstadoSync;
    private Button btnGuardar, btnRegresar, btnSincronizar;

    private HadogaDatabase db;
    private FirebaseFirestore firestore;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

        //  Inicializar elementos visuales
        inputNombre = findViewById(R.id.inputNombre);
        textNombreActual = findViewById(R.id.textNombreActual);
        textEstadoSync = findViewById(R.id.textEstadoSync);
        btnGuardar = findViewById(R.id.btnGuardarNombre);
        btnRegresar = findViewById(R.id.btnRegresar);
        btnSincronizar = findViewById(R.id.btnSincronizar);

        db = HadogaDatabase.getInstance(this);
        firestore = FirebaseFirestore.getInstance();

        //  Recuperar el usuario logueado por su email
        String email = getIntent().getStringExtra("email");
        if (email != null) {
            usuario = db.usuarioDao().findByEmail(email);
        }

        //  Mostrar nombre actual y estado
        if (usuario != null) {
            textNombreActual.setText("Nombre actual: " + usuario.getNombreClinica());
            textEstadoSync.setText("Estado: " + usuario.getSyncStatus());
        }

        //  Botones
        btnGuardar.setOnClickListener(v -> guardarCambios());
        btnRegresar.setOnClickListener(v -> finish());
        btnSincronizar.setOnClickListener(v -> sincronizarPendientes());
    }


    //  GUARDAR CAMBIOS

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

        usuario.setNombreClinica(nuevoNombre);

        if (isNetworkAvailable()) {
            // Actualizar primero el estado local
            usuario.setSyncStatus("SINCRONIZADO");
            new Thread(() -> db.usuarioDao().update(usuario)).start();

            // Luego subir el objeto ya actualizado a Firestore
            firestore.collection("usuarios")
                    .document(usuario.getEmail())
                    .set(usuario)
                    .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                        textNombreActual.setText("Nombre actual: " + nuevoNombre);
                        textEstadoSync.setText("Estado: SINCRONIZADO");
                        inputNombre.setText("");
                        Toast.makeText(this, "Nombre actualizado (local + nube)", Toast.LENGTH_SHORT).show();
                    }))
                    .addOnFailureListener(e -> {
                        usuario.setSyncStatus("PENDIENTE");
                        new Thread(() -> db.usuarioDao().update(usuario)).start();

                        runOnUiThread(() -> {
                            textEstadoSync.setText("Estado: PENDIENTE");
                            Toast.makeText(this, "Error al sincronizar. Guardado como PENDIENTE.", Toast.LENGTH_SHORT).show();
                        });
                    });
        } else {
            //  Sin conexión → solo guardar localmente
            usuario.setSyncStatus("PENDIENTE");
            new Thread(() -> {
                db.usuarioDao().update(usuario);
                // Recargar desde la base local para tener la versión más actualizada
                usuario = db.usuarioDao().findByEmail(usuario.getEmail());
            }).start();

            runOnUiThread(() -> {
                textNombreActual.setText("Nombre actual: " + usuario.getNombreClinica());
                textEstadoSync.setText("Estado: PENDIENTE");
                Toast.makeText(this, "Sin conexión. Guardado localmente (PENDIENTE)", Toast.LENGTH_SHORT).show();
            });
        }
    }


    //  SINCRONIZAR PENDIENTES

    private void sincronizarPendientes() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Sin conexión. No se puede sincronizar.", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            List<Usuario> pendientes = db.usuarioDao().getPendingUsuarios();

            if (pendientes.isEmpty()) {
                runOnUiThread(() ->
                        Toast.makeText(this, "No hay registros pendientes", Toast.LENGTH_SHORT).show());
                return;
            }

            for (Usuario u : pendientes) {
                // Obtener la versión más actualizada desde la base local
                Usuario usuarioActualizado = db.usuarioDao().findByEmail(u.getEmail());
                if (usuarioActualizado == null) continue;

                usuarioActualizado.setSyncStatus("SINCRONIZADO");
                db.usuarioDao().update(usuarioActualizado);

                firestore.collection("usuarios")
                        .document(usuarioActualizado.getEmail())
                        .set(usuarioActualizado)
                        .addOnSuccessListener(aVoid -> runOnUiThread(() -> {
                            // Si el usuario sincronizado es el que está viendo el perfil
                            if (usuario != null && usuario.getEmail().equals(usuarioActualizado.getEmail())) {
                                usuario = usuarioActualizado;
                                textNombreActual.setText("Nombre actual: " + usuario.getNombreClinica());
                                textEstadoSync.setText("Estado: SINCRONIZADO");
                            }

                            Toast.makeText(this, "Sincronizado: " + usuarioActualizado.getEmail(), Toast.LENGTH_SHORT).show();
                        }))
                        .addOnFailureListener(e -> {
                            usuarioActualizado.setSyncStatus("PENDIENTE");
                            db.usuarioDao().update(usuarioActualizado);
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Error al sincronizar: " + usuarioActualizado.getEmail(), Toast.LENGTH_SHORT).show());
                        });
            }
        }).start();
    }


    //  VERIFICAR CONEXIÓN

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo active = cm.getActiveNetworkInfo();
        return active != null && active.isConnected();
    }
}
