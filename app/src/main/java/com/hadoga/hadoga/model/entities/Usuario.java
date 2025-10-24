package com.hadoga.hadoga.model.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Tabla: usuarios
@Entity(
        tableName = "usuarios",
        indices = {@Index(value = "email", unique = true)}
)
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "nombre_clinica")
    private String nombreClinica;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "contrasena")
    private String contrasena;

    @ColumnInfo(name = "sync_status")
    private String syncStatus;

    public String getSyncStatus() { return syncStatus; }

    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    // Constructor con parámetros
    public Usuario(String nombreClinica, String email, String contrasena, String syncStatus) {
        this.nombreClinica = nombreClinica;
        this.email = email;
        this.contrasena = contrasena;
        this.syncStatus = syncStatus;
    }

    // Getters y setters del usuario
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombreClinica() {
        return nombreClinica;
    }

    public void setNombreClinica(String nombreClinica) {
        this.nombreClinica = nombreClinica;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }
}
