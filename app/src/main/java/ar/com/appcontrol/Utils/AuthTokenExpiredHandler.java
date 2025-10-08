package ar.com.appcontrol.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.android.volley.VolleyError;
import ar.com.appcontrol.LoginActivity;

/**
 * Manejador centralizado para detectar y manejar tokens JWT expirados
 */
public class AuthTokenExpiredHandler {

    /**
     * Verifica si un error de Volley es por token expirado (401 o 403)
     * Si es así, limpia la sesión y redirige al login
     *
     * @param context Contexto de la actividad
     * @param error Error de Volley a verificar
     * @return true si el error fue por autenticación (401/403), false en otro caso
     */
    public static boolean handleAuthError(Context context, VolleyError error) {
        if (error == null || error.networkResponse == null) {
            return false;
        }

        int statusCode = error.networkResponse.statusCode;

        // Detectar errores de autenticación
        if (statusCode == 401 || statusCode == 403) {
            handleTokenExpired(context);
            return true;
        }

        return false;
    }

    /**
     * Maneja el caso cuando el token ha expirado
     * Limpia todas las preferencias, el token JWT y redirige al login
     */
    private static void handleTokenExpired(Context context) {
        // Limpiar JWT token
        JWTManager jwtManager = new JWTManager(context);
        jwtManager.clearToken();

        // Limpiar SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Mostrar mensaje al usuario
        Toast.makeText(context, "Sesión expirada. Por favor inicie sesión nuevamente", Toast.LENGTH_LONG).show();

        // Redirigir al login
        redirectToLogin(context);
    }

    /**
     * Redirige al usuario a la pantalla de login
     * Limpia el stack de activities para que no pueda volver atrás
     */
    private static void redirectToLogin(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);

        // Si el contexto es una Activity, finalizarla
        if (context instanceof Activity) {
            ((Activity) context).finish();
        }
    }

    /**
     * Método público para forzar el logout y redirigir al login
     * Útil para botones de "Cerrar Sesión"
     */
    public static void logout(Context context) {
        handleTokenExpired(context);
    }
}
