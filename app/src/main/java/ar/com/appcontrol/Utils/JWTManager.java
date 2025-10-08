package ar.com.appcontrol.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class JWTManager {
    private static final String PREFS_NAME = "MisPreferencias";
    private static final String TOKEN_KEY = "jwt_token";
    private static final String TOKEN_EXPIRY_KEY = "jwt_expiry";

    private final SharedPreferences prefs;

    public JWTManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Guarda el token JWT en SharedPreferences
     */
    public void saveToken(String token) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(TOKEN_KEY, token);
        // Token expira en 24 horas (según tu middleware)
        long expiryTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        editor.putLong(TOKEN_EXPIRY_KEY, expiryTime);
        editor.commit(); // Usar commit() para escritura síncrona
    }

    /**
     * Obtiene el token JWT guardado
     */
    public String getToken() {
        return prefs.getString(TOKEN_KEY, null);
    }

    /**
     * Verifica si existe un token válido
     */
    public boolean hasValidToken() {
        String token = getToken();
        if (token == null || token.isEmpty()) {
            return false;
        }

        long expiryTime = prefs.getLong(TOKEN_EXPIRY_KEY, 0);
        return System.currentTimeMillis() < expiryTime;
    }

    /**
     * Limpia el token guardado (logout)
     */
    public void clearToken() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(TOKEN_KEY);
        editor.remove(TOKEN_EXPIRY_KEY);
        editor.commit(); // Usar commit() para escritura síncrona
    }
}
