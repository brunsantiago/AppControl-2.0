package ar.com.appcontrol.Utils;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * JsonObjectRequest con JWT automático en headers y manejo de errores de autenticación
 */
public class AuthenticatedJsonRequest extends JsonObjectRequest {

    private final String jwtToken;
    private final Context context;
    private final Response.ErrorListener userErrorListener;

    public AuthenticatedJsonRequest(int method, String url, JSONObject jsonRequest,
                                    String token,
                                    Response.Listener<JSONObject> listener,
                                    Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, createErrorListener(null, errorListener));
        this.jwtToken = token;
        this.context = null;
        this.userErrorListener = errorListener;
    }

    /**
     * Constructor con Context para manejo automático de errores de autenticación
     */
    public AuthenticatedJsonRequest(int method, String url, JSONObject jsonRequest,
                                    String token, Context context,
                                    Response.Listener<JSONObject> listener,
                                    Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, createErrorListener(context, errorListener));
        this.jwtToken = token;
        this.context = context;
        this.userErrorListener = errorListener;
    }

    /**
     * Crea un ErrorListener que intercepta errores 401/403 y redirige al login
     */
    private static Response.ErrorListener createErrorListener(Context context, Response.ErrorListener userErrorListener) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // Intentar manejar error de autenticación
                if (context != null && AuthTokenExpiredHandler.handleAuthError(context, error)) {
                    // El error fue manejado (token expirado), no llamar al listener del usuario
                    return;
                }

                // Si no fue error de autenticación, llamar al listener original
                if (userErrorListener != null) {
                    userErrorListener.onErrorResponse(error);
                }
            }
        };
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();

        if (jwtToken != null && !jwtToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + jwtToken);
        }

        headers.put("Content-Type", "application/json; charset=utf-8");

        return headers;
    }
}
