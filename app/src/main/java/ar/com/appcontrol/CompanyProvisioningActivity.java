package ar.com.appcontrol;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ar.com.appcontrol.POJO.Company;
import ar.com.appcontrol.Utils.PreferencesManager;

/**
 * Activity de provisioning para configuración inicial de empresa
 * Soporta 3 métodos:
 * 1. QR Code (visible en UI)
 * 2. Código Manual (visible en UI)
 * 3. Universal Link/Deep Link (automático, NO visible en UI - se activa cuando usuario hace click en link)
 */
public class CompanyProvisioningActivity extends AppCompatActivity {

    private CardView cardScanQR;
    private CardView cardManualCode;
    private ProgressBar progressBar;
    private View loadingOverlay;
    private PreferencesManager preferencesManager;
    private RequestQueue requestQueue;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_provisioning);

        // Inicializar componentes
        preferencesManager = new PreferencesManager(this);
        requestQueue = Volley.newRequestQueue(this);

        // Firebase ahora usa webadmin-4fa05 como proyecto principal
        storage = FirebaseStorage.getInstance();

        // Referencias a vistas
        cardScanQR = findViewById(R.id.cardScanQR);
        cardManualCode = findViewById(R.id.cardManualCode);
        progressBar = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Configurar listeners
        cardScanQR.setOnClickListener(v -> openQRScanner());
        cardManualCode.setOnClickListener(v -> showManualCodeDialog());

        // OPCIÓN 3: Deep Link / Universal Link (automático)
        // Verificar si la activity fue abierta por un deep link
        // Ejemplo: appcontrol://configure?token=XXX
        // O: https://app.appcontrol.com.ar/configure?token=XXX
        handleDeepLink(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Manejar deep links cuando la app ya está abierta
        handleDeepLink(intent);
    }

    /**
     * OPCIÓN 3: Manejo de Deep Links (Universal Link)
     * Este método se ejecuta automáticamente cuando el usuario hace click en un link de configuración
     * NO es visible en la UI, es completamente automático
     */
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();

        if (data != null) {
            String token = data.getQueryParameter("token");

            if (token != null && !token.isEmpty()) {
                // Usuario hizo click en un link de configuración
                // Validar token automáticamente
                validateToken(token);
            }
        }
    }

    /**
     * OPCIÓN 1: Abrir escáner de QR
     */
    private void openQRScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Escanea el código QR de configuración");
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    /**
     * OPCIÓN 2: Mostrar diálogo para código manual
     */
    private void showManualCodeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_code, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        EditText editTextCode = dialogView.findViewById(R.id.editTextActivationCode);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonValidate = dialogView.findViewById(R.id.buttonValidate);

        // Formatear automáticamente con guiones
        editTextCode.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;
            private int previousLength = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                previousLength = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                isFormatting = true;
                String text = s.toString().replace("-", "").toUpperCase();

                // Formato: XXXX-XXXX-XXXX-XXXX (4-4-4-4)
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < text.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append("-");
                    }
                    formatted.append(text.charAt(i));
                }

                editTextCode.removeTextChangedListener(this);
                editTextCode.setText(formatted.toString());
                editTextCode.setSelection(formatted.length());
                editTextCode.addTextChangedListener(this);

                isFormatting = false;
            }
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonValidate.setOnClickListener(v -> {
            String code = editTextCode.getText().toString().trim();

            if (code.isEmpty()) {
                Toast.makeText(this, "Ingresa un código válido", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
            validateActivationCode(code);
        });

        dialog.show();
    }

    /**
     * Resultado del escáner QR
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() != null) {
                String scannedData = result.getContents();

                // El QR contiene: appcontrol://configure?token=XXX
                // O directamente el token
                if (scannedData.startsWith("appcontrol://configure")) {
                    Uri uri = Uri.parse(scannedData);
                    String token = uri.getQueryParameter("token");
                    if (token != null) {
                        validateToken(token);
                    } else {
                        showError("QR inválido");
                    }
                } else {
                    // Asumir que el QR contiene directamente el token
                    validateToken(scannedData);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Validar token con API
     */
    private void validateToken(String token) {
        showLoading(true);

        String url = Configurador.API_PATH + "provisioning/validate-token";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("token", token);
        } catch (JSONException e) {
            e.printStackTrace();
            showLoading(false);
            showError("Error al preparar solicitud");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            int companyId = data.getInt("companyId");
                            String companyName = data.getString("companyName");

                            // Descargar logo antes de guardar configuración
                            downloadCompanyLogo(companyId, companyName);
                        } else {
                            showLoading(false);
                            showError("Token inválido");
                        }
                    } catch (JSONException e) {
                        showLoading(false);
                        showError("Error al procesar respuesta");
                    }
                },
                error -> {
                    showLoading(false);

                    String errorMsg = "Error de conexión";
                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String body = new String(error.networkResponse.data, "UTF-8");
                            JSONObject errorJson = new JSONObject(body);
                            JSONObject errorData = errorJson.getJSONObject("error");
                            errorMsg = errorData.getString("message");
                        }
                    } catch (Exception e) {
                        // Usar mensaje por defecto
                    }

                    showError(errorMsg);
                }
        );

        requestQueue.add(request);
    }

    /**
     * Validar código de activación manual
     */
    private void validateActivationCode(String activationCode) {
        showLoading(true);

        String url = Configurador.API_PATH + "provisioning/validate-token";

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("activationCode", activationCode);
        } catch (JSONException e) {
            e.printStackTrace();
            showLoading(false);
            showError("Error al preparar solicitud");
            return;
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    try {
                        boolean success = response.getBoolean("success");

                        if (success) {
                            JSONObject data = response.getJSONObject("data");
                            int companyId = data.getInt("companyId");
                            String companyName = data.getString("companyName");

                            // Descargar logo antes de guardar configuración
                            downloadCompanyLogo(companyId, companyName);
                        } else {
                            showLoading(false);
                            showError("Código inválido");
                        }
                    } catch (JSONException e) {
                        showLoading(false);
                        showError("Error al procesar respuesta");
                    }
                },
                error -> {
                    showLoading(false);

                    String errorMsg = "Error de conexión";
                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String body = new String(error.networkResponse.data, "UTF-8");
                            JSONObject errorJson = new JSONObject(body);
                            JSONObject errorData = errorJson.getJSONObject("error");
                            errorMsg = errorData.getString("message");
                        }
                    } catch (Exception e) {
                        // Usar mensaje por defecto
                    }

                    showError(errorMsg);
                }
        );

        requestQueue.add(request);
    }

    /**
     * Descarga ambos logos de la empresa desde Firebase Storage (principal y menú)
     */
    private void downloadCompanyLogo(int companyId, String companyName) {
        Company company = Company.getById(companyId);

        if (company == null) {
            // Si no hay empresa configurada, guardar sin logos
            saveCompanyConfigurationAndNavigate(companyId, companyName, null, null);
            return;
        }

        // Descargar logo principal
        String logoPath = company.getLogoStoragePath();
        String logoMenuPath = company.getLogoMenuStoragePath();

        if (logoPath == null && logoMenuPath == null) {
            // Si no hay logos configurados, guardar sin logos
            saveCompanyConfigurationAndNavigate(companyId, companyName, null, null);
            return;
        }

        // Descargar logo principal primero
        if (logoPath != null) {
            StorageReference logoRef = storage.getReference().child(logoPath);

            logoRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        String logoLocalPath = null;

                        if (bitmap != null) {
                            logoLocalPath = saveLogoToInternalStorage(bitmap, companyName, "company_logo.png");
                        }

                        // Ahora descargar logo del menú
                        downloadLogoMenu(companyId, companyName, logoLocalPath, logoMenuPath);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("CompanyProvisioning", "Error al descargar logo principal: " + e.getMessage());
                        // Continuar con el logo del menú
                        downloadLogoMenu(companyId, companyName, null, logoMenuPath);
                    }
                });
        } else {
            // Si no hay logo principal, ir directo al logo del menú
            downloadLogoMenu(companyId, companyName, null, logoMenuPath);
        }
    }

    /**
     * Descarga el logo del menú
     */
    private void downloadLogoMenu(int companyId, String companyName, String logoLocalPath, String logoMenuPath) {
        if (logoMenuPath == null) {
            // Si no hay logo del menú, guardar solo con el logo principal
            saveCompanyConfigurationAndNavigate(companyId, companyName, logoLocalPath, null);
            return;
        }

        StorageReference logoMenuRef = storage.getReference().child(logoMenuPath);

        logoMenuRef.getBytes(1024 * 1024)
            .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                @Override
                public void onSuccess(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    String logoMenuLocalPath = null;

                    if (bitmap != null) {
                        logoMenuLocalPath = saveLogoToInternalStorage(bitmap, companyName, "company_logo_menu.png");
                    }

                    // Guardar configuración con ambos logos
                    saveCompanyConfigurationAndNavigate(companyId, companyName, logoLocalPath, logoMenuLocalPath);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e("CompanyProvisioning", "Error al descargar logo del menú: " + e.getMessage());
                    // Si falla, guardar solo con el logo principal
                    saveCompanyConfigurationAndNavigate(companyId, companyName, logoLocalPath, null);
                }
            });
    }

    /**
     * Guarda el logo en el almacenamiento interno
     * @param bitmap Bitmap del logo a guardar
     * @param companyName Nombre de la empresa
     * @param fileName Nombre del archivo (ej: "company_logo.png" o "company_logo_menu.png")
     * @return Ruta absoluta del archivo guardado
     */
    private String saveLogoToInternalStorage(Bitmap bitmap, String companyName, String fileName) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("companyLogos", Context.MODE_PRIVATE);
        File logoFile = new File(directory, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(logoFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            return logoFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Guarda la configuración y navega al LoginActivity
     */
    private void saveCompanyConfigurationAndNavigate(int companyId, String companyName, String logoPath, String logoMenuPath) {
        // Guardar configuración
        if (logoPath != null && logoMenuPath != null) {
            preferencesManager.saveCompanyConfiguration(companyId, companyName, logoPath, logoMenuPath);
        } else if (logoMenuPath != null) {
            preferencesManager.saveCompanyConfiguration(companyId, companyName, null, logoMenuPath);
        } else if (logoPath != null) {
            preferencesManager.saveCompanyConfiguration(companyId, companyName, logoPath);
        } else {
            preferencesManager.saveCompanyConfiguration(companyId, companyName);
        }

        // Configurar Configurador
        Configurador.setIdEmpresa(companyId);

        // Navegar a LoginActivity
        showLoading(false);
        showSuccessAndNavigate(companyName);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccessAndNavigate(String companyName) {
        Toast.makeText(this, "Empresa configurada: " + companyName, Toast.LENGTH_LONG).show();

        // Esperar 1 segundo y navegar a LoginActivity
        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(CompanyProvisioningActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }, 1000);
    }

    @Override
    public void onBackPressed() {
        // No permitir volver atrás durante el provisioning
        // El usuario DEBE configurar una empresa
    }
}
