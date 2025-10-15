package ar.com.appcontrol;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import ar.com.appcontrol.POJO.Company;
import ar.com.appcontrol.Utils.PreferencesManager;

/**
 * Activity de inicio que decide el flujo de navegación:
 * - Si empresa NO configurada → CompanyProvisioningActivity
 * - Si empresa YA configurada → LoginActivity
 */
public class LauncherActivity extends AppCompatActivity {

    // DEBUG: Cambiar para forzar una empresa específica durante testing
    // null = flujo normal, "SAB-5" / "CONSISA" / "BROUCLEAN" = forzar empresa
    private static final String DEBUG_COMPANY = "SAB-5";

    private static final int SPLASH_DELAY = 1000; // 1 segundo

    private PreferencesManager preferencesManager;
    private FirebaseStorage storage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No necesita layout, es solo lógica de navegación
        // Opcionalmente se puede crear un layout con logo o splash

        preferencesManager = new PreferencesManager(this);
        storage = FirebaseStorage.getInstance();

        new Handler().postDelayed(() -> {
            // Modo debug: forzar empresa si DEBUG_COMPANY está configurado
            if (DEBUG_COMPANY != null) {
                int debugCompanyId = getCompanyIdFromName(DEBUG_COMPANY);

                // Descargar logo antes de guardar configuración
                downloadCompanyLogoForDebug(debugCompanyId, DEBUG_COMPANY);
                return;
            }

            // Flujo normal
            if (!preferencesManager.isCompanyConfigured()) {
                // Primera vez - ir a provisioning
                Intent intent = new Intent(LauncherActivity.this, CompanyProvisioningActivity.class);
                startActivity(intent);
            } else {
                // Ya configurado - cargar empresa y continuar a Login
                int companyId = preferencesManager.getSelectedCompanyId();
                Configurador.setIdEmpresa(companyId);

                Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
                startActivity(intent);
            }
            finish();
        }, SPLASH_DELAY);
    }

    private int getCompanyIdFromName(String companyName) {
        switch (companyName) {
            case "SAB-5":
                return 1;
            case "CONSISA":
                return 2;
            case "BROUCLEAN":
                return 3;
            default:
                return 1;
        }
    }

    /**
     * Descarga ambos logos de la empresa para modo DEBUG (principal y menú)
     */
    private void downloadCompanyLogoForDebug(int companyId, String companyName) {
        Company company = Company.getById(companyId);

        if (company == null) {
            Log.w("LauncherActivity", "No hay empresa configurada");
            saveAndNavigate(companyId, companyName, null, null);
            return;
        }

        String logoPath = company.getLogoStoragePath();
        String logoMenuPath = company.getLogoMenuStoragePath();

        if (logoPath == null && logoMenuPath == null) {
            Log.w("LauncherActivity", "No hay logos configurados para la empresa");
            saveAndNavigate(companyId, companyName, null, null);
            return;
        }

        Log.d("LauncherActivity", "=== INICIO DESCARGA LOGOS ===");
        Log.d("LauncherActivity", "Company ID: " + companyId);
        Log.d("LauncherActivity", "Company Name: " + companyName);
        Log.d("LauncherActivity", "Logo Path: " + logoPath);
        Log.d("LauncherActivity", "Logo Menu Path: " + logoMenuPath);
        Log.d("LauncherActivity", "Storage Bucket: " + storage.getReference().getBucket());

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
                        downloadLogoMenuForDebug(companyId, companyName, logoLocalPath, logoMenuPath);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e("LauncherActivity", "=== ERROR DESCARGA LOGO PRINCIPAL ===");
                        Log.e("LauncherActivity", "Error: " + e.getMessage());
                        e.printStackTrace();
                        // Continuar con el logo del menú
                        downloadLogoMenuForDebug(companyId, companyName, null, logoMenuPath);
                    }
                });
        } else {
            // Si no hay logo principal, ir directo al logo del menú
            downloadLogoMenuForDebug(companyId, companyName, null, logoMenuPath);
        }
    }

    /**
     * Descarga el logo del menú para modo DEBUG
     */
    private void downloadLogoMenuForDebug(int companyId, String companyName, String logoLocalPath, String logoMenuPath) {
        if (logoMenuPath == null) {
            // Si no hay logo del menú, guardar solo con el logo principal
            saveAndNavigate(companyId, companyName, logoLocalPath, null);
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
                    saveAndNavigate(companyId, companyName, logoLocalPath, logoMenuLocalPath);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    Log.e("LauncherActivity", "=== ERROR DESCARGA LOGO MENU ===");
                    Log.e("LauncherActivity", "Error: " + e.getMessage());
                    e.printStackTrace();
                    // Si falla, guardar solo con el logo principal
                    saveAndNavigate(companyId, companyName, logoLocalPath, null);
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
     * Guarda la configuración y navega a LoginActivity
     */
    private void saveAndNavigate(int companyId, String companyName, String logoPath, String logoMenuPath) {
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
        Intent intent = new Intent(LauncherActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
