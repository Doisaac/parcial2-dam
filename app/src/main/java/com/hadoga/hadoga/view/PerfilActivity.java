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
            new Thread(() -> db.usuarioDao().update(usuario)).start();

            runOnUiThread(() -> {
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

            final int totalPendientes = pendientes.size();
            final int[] completadas = {0};

            for (Usuario u : pendientes) {
                // Cambiar estado antes de enviar a Firestore
                u.setSyncStatus("SINCRONIZADO");
                new Thread(() -> db.usuarioDao().update(u)).start();

                firestore.collection("usuarios")
                        .document(u.getEmail())
                        .set(u)
                        .addOnSuccessListener(aVoid -> {
                            runOnUiThread(() -> {
                                if (usuario != null && usuario.getEmail().equals(u.getEmail())) {
                                    usuario.setSyncStatus("SINCRONIZADO");
                                    textEstadoSync.setText("Estado: SINCRONIZADO");
                                }
                                Toast.makeText(this, "Sincronizado: " + u.getEmail(), Toast.LENGTH_SHORT).show();
                            });

                            completadas[0]++;
                            if (completadas[0] == totalPendientes) {
                                runOnUiThread(() -> {
                                    Usuario actualizado = db.usuarioDao().findByEmail(usuario.getEmail());
                                    if (actualizado != null) {
                                        usuario = actualizado;
                                        textEstadoSync.setText("Estado: " + usuario.getSyncStatus());
                                    }
                                    Toast.makeText(this, "Sincronización completada", Toast.LENGTH_SHORT).show();
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Si falla, revertir estado a PENDIENTE
                            u.setSyncStatus("PENDIENTE");
                            new Thread(() -> db.usuarioDao().update(u)).start();

                            runOnUiThread(() ->
                                    Toast.makeText(this, "Error al sincronizar: " + u.getEmail(), Toast.LENGTH_SHORT).show());
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
