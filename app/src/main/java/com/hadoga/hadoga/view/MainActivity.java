package com.hadoga.hadoga.view;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.hadoga.hadoga.R;
import com.hadoga.hadoga.view.fragments.DashboardFragment;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configurar Toolbar (parte superior)
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Listener del menú de la Toolbar
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        // Obtener el correo del usuario logueado (enviado desde LoginActivity)
        userEmail = getIntent().getStringExtra("email");

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Cargar fragmento inicial
        loadFragment(new DashboardFragment());

        // Listener para el menú inferior
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.menu_dashboard) {
                selectedFragment = new DashboardFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    //  Acción al seleccionar elementos del menú superior (Toolbar)
    private boolean onMenuItemClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_perfil) {
            Intent intent = new Intent(MainActivity.this, PerfilActivity.class);
            intent.putExtra("email", userEmail);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }
}
