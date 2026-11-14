package com.utp.tienda.patrones.singleton;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Patrón Creacional: Singleton
 * Garantiza que la clase tenga una única instancia y proporciona un punto de acceso global a ella.
 */
public class DatabaseSingleton {

    // [BLOQUE 1: ATRIBUTO ESTÁTICO PRIVADO]
    private static DatabaseSingleton instance;
    private Connection connection;

    // Credenciales leídas desde variables de entorno (Render o .env local; nunca en código).
    // Referencia local:
    //   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/bd_sistema_negocio
    //   SPRING_DATASOURCE_USERNAME=postgres
    //   SPRING_DATASOURCE_PASSWORD=tu_password_local
    private static String env(String nombre) {
        String valor = System.getenv(nombre);
        return valor != null ? valor.trim() : "";
    }

    // [BLOQUE 2: CONSTRUCTOR PRIVADO]
    private DatabaseSingleton() {
        String url = env("SPRING_DATASOURCE_URL");
        String user = env("SPRING_DATASOURCE_USERNAME");
        String password = env("SPRING_DATASOURCE_PASSWORD");
        if (url.isBlank() || user.isBlank() || password.isBlank()) {
            System.err.println("[DatabaseSingleton] Faltan variables SPRING_DATASOURCE_URL/USERNAME/PASSWORD.");
            return;
        }
        try {
            Class.forName("org.postgresql.Driver");
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("[DatabaseSingleton] Error conectando a PostgreSQL: " + e.getMessage());
        }
    }

    // [BLOQUE 3: MÉTODO GLOBAL SINCRONIZADO]
    public static synchronized DatabaseSingleton getInstance() {
        try {
            if (instance == null || instance.getConnection() == null || instance.getConnection().isClosed()) {
                instance = new DatabaseSingleton();
            }
        } catch (SQLException e) {
            instance = new DatabaseSingleton();
        }
        return instance;
    }

    // [BLOQUE 4: ACCESO A LA CONEXIÓN]
    public Connection getConnection() {
        return this.connection;
    }
}

