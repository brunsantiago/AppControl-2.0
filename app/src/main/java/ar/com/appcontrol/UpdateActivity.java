package ar.com.appcontrol;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;

public class UpdateActivity extends AppCompatActivity {

    private static final int REQUEST_INSTALL_PERMISSION = 1001;

    private FirebaseStorage storage;
    private String versionNameServer;
    private ProgressBar progressBar;
    private TextView progressBarNumber;
    private String androidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);

        androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        storage = FirebaseStorage.getInstance();
        versionNameServer = getIntent().getExtras().getString("versionNameServer");
        progressBar = findViewById(R.id.progressBar);
        progressBarNumber = findViewById(R.id.progressBarNumber);

        // Verificar permisos antes de descargar
        if (canRequestPackageInstalls()) {
            // Tiene permiso - Proceder con la descarga
            downloadUpdateApp();
        } else {
            // No tiene permiso - Solicitarlo primero
            requestInstallPermission();
        }

    }


    private void downloadUpdateApp(){

        String fileName = "app-control.apk";

        // Crear directorio específico para updates en caché
        File updateDir = new File(getCacheDir(), "updates");
        if (!updateDir.exists()) {
            updateDir.mkdirs();
        }

        // Usar nombre de archivo con versión específica
        File apkFile = new File(updateDir, "app-control-v" + versionNameServer + ".apk");

        // Verificar si el archivo ya existe y es válido
        if (apkFile.exists() && apkFile.length() > 0) {
            // APK ya descargado - Usar versión en caché
            Toast.makeText(this, "Usando versión descargada previamente", Toast.LENGTH_SHORT).show();
            installApk(apkFile);
            return;
        }

        // Limpiar versiones antiguas antes de descargar
        cleanOldApkFiles(updateDir, apkFile);

        // Obtener referencia de Firebase Storage
        StorageReference updateRef = storage.getReference()
                //.child("BROUCLEAN") or .child("CONSISA")
                .child("RELEASE") // SAB-5 Directorio raiz
                .child(versionNameServer)
                .child(fileName);

        // Descargar APK
        updateRef.getFile(apkFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Descarga exitosa - Instalar
                installApk(apkFile);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(UpdateActivity.this, "Error al intentar actualizar ", Toast.LENGTH_SHORT).show();
                // Limpiar archivo parcial si existe
                if (apkFile.exists()) {
                    apkFile.delete();
                }
                finish();
            }
        }).addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                progressBarNumber.setText((int) progress+" %");
            }
        });

    }

    private Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    /**
     * Verifica si la aplicación tiene permiso para instalar paquetes
     * @return true si tiene permiso, false en caso contrario
     */
    private boolean canRequestPackageInstalls() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPackageManager().canRequestPackageInstalls();
        }
        return true; // Versiones anteriores a Android 8.0 no requieren este permiso
    }

    /**
     * Solicita al usuario el permiso para instalar aplicaciones desconocidas
     */
    private void requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ThemeDialogCustom);
            LayoutInflater inflater = getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.custom_alert_permission_request, null))
                    .setPositiveButton("Continuar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Abrir configuración de permisos
                            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivityForResult(intent, REQUEST_INSTALL_PERMISSION);
                        }
                    })
                    .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showPermissionDeniedDialog();
                        }
                    })
                    .setCancelable(false);
            builder.create().show();
        }
    }

    /**
     * Maneja el resultado de la solicitud de permiso
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_INSTALL_PERMISSION) {
            if (canRequestPackageInstalls()) {
                // Usuario concedió el permiso - Proceder con la descarga
                downloadUpdateApp();
            } else {
                // Usuario rechazó el permiso - Mostrar mensaje y cerrar
                showPermissionDeniedDialog();
            }
        }
    }

    /**
     * Muestra un diálogo informando que no se puede actualizar sin permisos
     */
    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.ThemeDialogCustom);
        LayoutInflater inflater = getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.custom_alert_permission_denied, null))
                .setPositiveButton("Entendido", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false);
        builder.create().show();
    }

    /**
     * Elimina archivos APK antiguos del directorio de updates
     * @param updateDir Directorio donde se almacenan los updates
     * @param currentApkFile El archivo APK actual que se está usando/descargando
     */
    private void cleanOldApkFiles(File updateDir, File currentApkFile) {
        if (updateDir == null || !updateDir.exists() || !updateDir.isDirectory()) {
            return;
        }

        File[] oldApks = updateDir.listFiles((dir, name) -> name.endsWith(".apk"));

        if (oldApks != null) {
            for (File oldApk : oldApks) {
                // Eliminar todos los APKs excepto el que vamos a descargar/usar
                if (!oldApk.getAbsolutePath().equals(currentApkFile.getAbsolutePath())) {
                    boolean deleted = oldApk.delete();
                    if (deleted) {
                        Log.d("UpdateActivity", "Eliminado APK antiguo: " + oldApk.getName());
                    }
                }
            }
        }
    }

    /**
     * Instala el APK desde el archivo especificado
     * @param apkFile Archivo APK a instalar
     */
    private void installApk(File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            Toast.makeText(getApplicationContext(), "No se encuentra el archivo de instalación", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(
                uriFromFile(getApplicationContext(), apkFile),
                "application/vnd.android.package-archive"
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            getApplicationContext().startActivity(intent);
            finish();
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("UpdateActivity", "Error al abrir el instalador del APK", e);
            Toast.makeText(this, "No se puede instalar la actualización", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}