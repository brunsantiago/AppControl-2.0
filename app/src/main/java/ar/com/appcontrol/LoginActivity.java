package ar.com.appcontrol;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import ar.com.appcontrol.AlertDialog.DeviceAlertError;
import ar.com.appcontrol.AlertDialog.ConnectionAlert;
import ar.com.appcontrol.AlertDialog.InternetConnectionAlert;
import ar.com.appcontrol.AlertDialog.ServerAlertError;
import ar.com.appcontrol.AlertDialog.UpdateAlert;
import ar.com.appcontrol.Utils.AuthenticatedJsonArrayRequest;
import ar.com.appcontrol.Utils.AuthenticatedJsonRequest;
import ar.com.appcontrol.Utils.JWTManager;
import ar.com.appcontrol.Utils.PreferencesManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class LoginActivity extends AppCompatActivity {

    private static final String NOMBRE_PERSONAL = "an";
    private static final String PERS_CODI = "pers_codi";
    private static final String USER_PROFILE = "user_profile";
    private static final String MAP_COOR = "map_coor";
    private static final String MAP_RADIO = "map_radio";
    private static final String DEVI_UBIC = "devi_ubic";

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private TextView textViewNroLegajo;
    private TextView textViewClave;
    private Button botonIngresar;
    private TextView textViewRecoveryKey;
    private TextView textViewRegistrarse;
    private TextView textViewVersion;
    private ImageView imageViewCompanyLogo;
    private ProgressDialog progressDialog = null;
    private UpdateAlert currentUpdateAlert = null;
    private PreferencesManager preferencesManager;

    private static final String NRO_LEGAJO = "nl";

    private static final String NOMBRE_CLIENTE = "nombreCliente";
    private static final String NOMBRE_OBJETIVO = "nombreObjetivo";
    private static final String ID_CLIENTE = "idCliente";
    private static final String ID_OBJETIVO = "idObjetivo";
    private static final String ID_ANDROID = "androidId";

    private FirebaseStorage storage;
    private static final String PROFILE_PHOTO = "ProfilePhotoPath";

    private JWTManager jwtManager;

    private int versionCodeApp = BuildConfig.VERSION_CODE;
    private String versionNameApp = BuildConfig.VERSION_NAME;
    private int versionCodeServer;
    private String versionNameServer;
    private int versionPriority;
    private String androidId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        setContentView(R.layout.activity_login);

        androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        jwtManager = new JWTManager(this);
        preferencesManager = new PreferencesManager(this);

        textViewNroLegajo = findViewById(R.id.nrolegajo);
        textViewClave = findViewById(R.id.clave);
        botonIngresar = findViewById(R.id.ingresar);
        textViewRecoveryKey = findViewById(R.id.recoverKey);
        textViewRegistrarse = findViewById(R.id.registrarse);
        textViewVersion = findViewById(R.id.version);
        imageViewCompanyLogo = findViewById(R.id.companyLogo);

        storage = FirebaseStorage.getInstance();

        // Cargar logo de la empresa
        loadCompanyLogo();

        progressDialog = new ProgressDialog(LoginActivity.this);
        progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        progressDialog.setCancelable(false);

        textViewVersion.setText("Version "+versionNameApp);

        botonIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                botonIngresar.setClickable(false);
                String nroLegajo = textViewNroLegajo.getText().toString();
                String clave = textViewClave.getText().toString();
                if((nroLegajo!=null && !nroLegajo.equals(""))&&(clave!=null && !clave.equals(""))){
                    checkConnections(nroLegajo,clave);
                } else {
                    botonIngresar.setClickable(true);
                    Toast.makeText(LoginActivity.this, "Por favor ingrese un Numero de Legajo y/o Clave valida", Toast.LENGTH_SHORT).show();
                }
            }
        });

        textViewRecoveryKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,RecoveryKeyActivity.class);
                startActivity(intent);
                finish();
            }
        });

        textViewRegistrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this,RegistroActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Solicitar permiso de cámara al iniciar
        requestCameraPermission();

    }

    /**
     * Solicita el permiso de cámara si no ha sido otorgado
     */
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // El permiso no ha sido otorgado, solicitarlo
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
        // Si el permiso ya fue otorgado, no hacer nada
    }

    /**
     * Maneja la respuesta del usuario a la solicitud de permiso
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso otorgado
                Log.d("LoginActivity", "Permiso de cámara otorgado");
            } else {
                // Permiso denegado - la app funcionará pero sin funcionalidad de cámara
                Toast.makeText(this,
                    "El permiso de cámara es necesario para usar el reconocimiento facial",
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        sharedPreferencesClear();
        deleteCache(this);
        loadCompanyLogo();
        checkForUpdates();
        super.onResume();
    }

    /**
     * Carga el logo de la empresa desde el almacenamiento local
     */
    private void loadCompanyLogo() {
        String logoPath = preferencesManager.getCompanyLogoPath();

        if (logoPath != null && !logoPath.isEmpty()) {
            File logoFile = new File(logoPath);

            if (logoFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(logoPath);
                if (bitmap != null) {
                    imageViewCompanyLogo.setImageBitmap(bitmap);
                } else {
                    // Si no se puede cargar el bitmap, usar logo por defecto
                    imageViewCompanyLogo.setImageResource(R.mipmap.ic_launcher);
                }
            } else {
                // Si el archivo no existe, usar logo por defecto
                imageViewCompanyLogo.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            // Si no hay logo guardado, usar logo por defecto
            imageViewCompanyLogo.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private void sharedPreferencesClear(){
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void initUserData(String nroLegajo, String token){

        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String mJSONURLString = Configurador.API_PATH + "personal/"+nroLegajo+"/"+Configurador.getIdEmpresaString();

        AuthenticatedJsonArrayRequest jsonArrayRequest = new AuthenticatedJsonArrayRequest(
                mJSONURLString,
                token,
                this,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if(response.length()>0) {
                            JSONObject jsonObject;
                            try {
                                jsonObject = response.getJSONObject(0);
                                String persSector = jsonObject.getString("PERS_SECT");
                                String persEgreso = jsonObject.getString("PERS_FEGR");
                                if(isUserEnable(persSector,persEgreso)){
                                    String nombre = jsonObject.getString("PERS_NOMB");
                                    String persCodi = jsonObject.getString("PERS_CODI");
                                    setUserProfile(Integer.parseInt(persCodi), token);
                                    editor.putString(NRO_LEGAJO,nroLegajo);
                                    editor.putString(NOMBRE_PERSONAL, nombre);
                                    editor.putString(PERS_CODI, persCodi);
                                    editor.apply();
                                    //progressDialog.dismiss();
                                    loadDevice();
                                }else{
                                    Toast.makeText(LoginActivity.this, "Usuario inhabilitado", Toast.LENGTH_SHORT).show();
                                    botonIngresar.setClickable(true);
                                    progressDialog.dismiss();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(LoginActivity.this, "Error de Servidor", Toast.LENGTH_SHORT).show();
                                botonIngresar.setClickable(true);
                                progressDialog.dismiss();
                            }
                        }else{
                            Toast.makeText(LoginActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            botonIngresar.setClickable(true);
                            progressDialog.dismiss();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        Toast.makeText(LoginActivity.this, "Error de Servidor", Toast.LENGTH_SHORT).show();
                        botonIngresar.setClickable(true);
                        progressDialog.dismiss();
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);

    }

    public void setUserProfile(int persCodi, String token){
        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String mJSONURLString = Configurador.API_PATH + "users/"+persCodi+"/"+Configurador.getIdEmpresaString();
        AuthenticatedJsonRequest jsonObjectRequest = new AuthenticatedJsonRequest(
                Request.Method.GET,
                mJSONURLString,
                null,
                token,
                this,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String userProfile = "";
                        try {
                            userProfile = response.getString("result");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        editor.putString(USER_PROFILE, userProfile);
                        editor.apply();
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        editor.putString(USER_PROFILE, "");
                        editor.apply();
                    }
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void signIn(String nroLegajo, String clave) {

        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String URL = Configurador.API_PATH + "login/"+Configurador.getIdEmpresaString();
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("user_lega", nroLegajo);
            jsonBody.put("user_pass", clave);

            final String requestBody = jsonBody.toString();

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, URL, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getString("result").equals("CORRECT_LOGIN")){
                            // Limpiar preferencias ANTES de guardar el nuevo token
                            sharedPreferencesClear();

                            // Guardar JWT token
                            if(response.has("token")){
                                String token = response.getString("token");
                                jwtManager.saveToken(token);

                                initUserData(nroLegajo, token);
                            }else{
                                Toast.makeText(LoginActivity.this, "Token no recibido", Toast.LENGTH_SHORT).show();
                            }
                        }else if(response.getString("result").equals("INCORRECT_LOGIN")){
                            Toast.makeText(LoginActivity.this, "Numero de legajo y/o clave incorrectas", Toast.LENGTH_SHORT).show();
                            botonIngresar.setClickable(true);
                            progressDialog.dismiss();
                        }else if(response.getString("result").equals("NOT_FOUND")){
                            Toast.makeText(LoginActivity.this, "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                            botonIngresar.setClickable(true);
                            progressDialog.dismiss();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        botonIngresar.setClickable(true);
                        progressDialog.dismiss();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(LoginActivity.this, "Error al iniciar sesion", Toast.LENGTH_SHORT).show();
                    botonIngresar.setClickable(true);
                    progressDialog.dismiss();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }
            };
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            botonIngresar.setClickable(true);
            progressDialog.dismiss();
            e.printStackTrace();
        }
    }

    private boolean isUserEnable(String persSector, String persEgreso){
        if(persSector.equals("3")){
            return false;
        }else return persEgreso.equals("null");
    }

    private String getAndroidID(){
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

    private void loadDevice(){

        String idAndroid = getAndroidID();
        if(idAndroid!=null){
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String mJSONURLString = Configurador.API_PATH + "devices/"+idAndroid+"/"+Configurador.getIdEmpresaString();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET,
                    mJSONURLString,
                    null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (response != null) {

                                String nombreCliente = null;
                                String nombreObjetivo = null;
                                boolean disabledDevice = false;
                                try {
                                    if (response.getString("DEVI_ESTA").equals("ACTIVO")) {
                                        nombreCliente = response.getString("DEVI_NCLI");
                                        nombreObjetivo = response.getString("DEVI_NOBJ");
                                        String idCliente = response.getString("DEVI_CCLI");
                                        String idObjetivo = response.getString("DEVI_COBJ");
                                        int ubicacion = response.getInt("DEVI_UBIC");
                                        String mapCoordenada = response.getString("DEVI_COOR");
                                        int mapRadio = response.getInt("DEVI_RADI");

                                        SharedPreferences prefs = getSharedPreferences("MisPreferencias", MODE_PRIVATE);

                                        SharedPreferences.Editor editor = prefs.edit();
                                        editor.putString(NOMBRE_CLIENTE, nombreCliente);
                                        editor.putString(NOMBRE_OBJETIVO, nombreObjetivo);
                                        editor.putString(ID_CLIENTE, idCliente);
                                        editor.putString(ID_OBJETIVO, idObjetivo);
                                        editor.putInt(DEVI_UBIC, ubicacion);
                                        editor.putString(MAP_COOR, mapCoordenada);
                                        editor.putInt(MAP_RADIO, mapRadio);
                                        editor.putString(ID_ANDROID, idAndroid);
                                        editor.apply();


                                        loadLoginActivity();
                                    }else{
                                        disabledDevice = true;
                                    }
                                } catch (JSONException e) {
                                    loadLoginActivity();
                                }
                                if(nombreCliente==null || nombreObjetivo==null){
                                    if(!disabledDevice){
                                        loadLoginActivity();
                                    }else{
                                        resetFormLogin();
                                        progressDialog.dismiss();
                                        showDeviceErrorAlert();
                                    }
                                }
                            }else{
                                loadLoginActivity();
                            }
                        }
                    },
                    new Response.ErrorListener(){
                        @Override
                        public void onErrorResponse(VolleyError error){
                            loadLoginActivity();
                        }
                    });
            requestQueue.add(jsonObjectRequest);
        }else{
            loadLoginActivity();
        }
    }

    private void loadLoginActivity(){
        downloadProfilePhoto();
    }

    private void resetFormLogin(){
        botonIngresar.setClickable(true);
        textViewNroLegajo.setText("");
        textViewClave.setText("");
    }

    private void downloadProfilePhoto(){

        SharedPreferences prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
        String nroLegajo = prefs.getString(NRO_LEGAJO,"");
        String fileName = nroLegajo+"_profile_photo.jpg";
        String companyName = preferencesManager.getSelectedCompanyName();

        // Log de debug
        Log.d("LoginActivity", "Descargando foto - CompanyName: " + companyName + ", Legajo: " + nroLegajo);
        // Ruta: accounts/SB9mPqR7sLwN2vH8Tz3F/entities/{companyName}/users/profile_photos/{nroLegajo}/{fileName}
        // Usando Firebase Storage principal (webadmin-4fa05)
        StorageReference photoRef = storage.getReference()
                .child("accounts")
                .child("SB9mPqR7sLwN2vH8Tz3F")
                .child("entities")
                .child(companyName != null ? companyName : "")
                .child("users")
                .child("profile_photos")
                .child(nroLegajo)
                .child(fileName);

        Log.d("LoginActivity", "Ruta completa: " + photoRef.getPath());

        photoRef.getBytes(600*600)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                        saveToInternalStorage(bitmap);
                    }
                })
                .addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@androidx.annotation.NonNull Exception e) {
                        // Si falla la descarga de la foto, mostrar mensaje y detener el progreso
                        Log.e("LoginActivity", "Error al descargar foto de perfil: " + e.getMessage());
                        progressDialog.dismiss();
                        botonIngresar.setClickable(true);
                        Toast.makeText(LoginActivity.this, "No se pudo descargar la foto de perfil", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveToInternalStorage(Bitmap bitmapImage){

        SharedPreferences prefs = getSharedPreferences("MisPreferencias",MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory,"profile.jpg");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        editor.putString(PROFILE_PHOTO, directory.getAbsolutePath());
        editor.apply();

        loadMainActivity();
    }

    private void loadMainActivity(){
        progressDialog.dismiss();
        Intent intent = new Intent(LoginActivity.this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void checkForUpdates(){

        updateVersionDevice(); // Actualiza la version del dispositivo en el servidor

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String mJSONURLString = Configurador.API_PATH + "app_version/last_version/"+Configurador.getIdEmpresaString();
        //String mJSONURLString = Configurador.API_PATH + "test/app_version/last_version/"+Configurador.getIdEmpresaString();
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                mJSONURLString,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        if(response.length()>0) {
                            JSONObject jsonObject;
                            try {
                                jsonObject = response.getJSONObject(0);
                                versionCodeServer = jsonObject.getInt("version_code");
                                versionNameServer = jsonObject.getString("version_name");
                                versionPriority = jsonObject.getInt("version_priority");
                                if(versionCodeApp<versionCodeServer){
                                    if(versionPriority==0){ // Alta Prioridad
                                        Intent intent = new Intent(LoginActivity.this, UpdateActivity.class);
                                        intent.putExtra("versionNameServer", versionNameServer);
                                        startActivity(intent);
                                    }else{ // Baja Prioridad
                                        showUpdateAlert(versionNameServer);
                                    }
                                }
                            } catch (JSONException e) {
                                Toast.makeText(LoginActivity.this, "Error en Actualizacion", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            //Toast.makeText(LoginActivity.this, "No hay actualizaciones pendientes", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        //Toast.makeText(LoginActivity.this, "Error de Servidor", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        requestQueue.add(jsonArrayRequest);

    }

    private void updateVersionDevice(){

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        String URL = Configurador.API_PATH + "devices/"+androidId+"/"+Configurador.getIdEmpresaString();
        JSONObject jsonBody = new JSONObject();

        try {
            jsonBody.put("appVersion", versionNameApp);
            final String requestBody = jsonBody.toString();
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PATCH, URL, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        if(response.getInt("result")==1){
                            // Si sale OK !
                            //Toast.makeText(LoginActivity.this, "versionNameServer: "+versionNameServer+"Android ID: "+androidId, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        //Toast.makeText(LoginActivity.this, "No se pudo cargar la version en el dispositivo", Toast.LENGTH_SHORT).show();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    //Toast.makeText(LoginActivity.this, "No se pudo cargar la version en el dispositivo", Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }
            };
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cerrar diálogo de actualización cuando la Activity pasa a segundo plano
        if (currentUpdateAlert != null) {
            try {
                currentUpdateAlert.dismiss();
            } catch (Exception e) {
                Log.e("LoginActivity", "Error al cerrar diálogo: " + e.getMessage());
            }
            currentUpdateAlert = null;
        } else {
            Log.d("LoginActivity", "No hay diálogo para cerrar (currentUpdateAlert es null)");
        }
    }

    @Override
    protected void onDestroy() {
        deleteCache(this);
        super.onDestroy();
    }

    private void checkConnections(String nroLegajo, String clave){
        progressDialog.show();
        progressDialog.setContentView(R.layout.custom_progressdialog);
        if (NetworkUtil.isInternetAvailable(this)) {
            // Verificar si la conexión es activa realizando una solicitud
            new Thread(() -> {
                boolean activeConnection = NetworkUtil.isConnectionActive("https://www.google.com");
                runOnUiThread(() -> {
                    if (activeConnection) {
                        //Conexión a Internet activa
                        serverConnection(nroLegajo,clave);
                    } else {
                        //Conexión a Internet inactiva
                        botonIngresar.setClickable(true);
                        progressDialog.dismiss();
                        showInternetConectionAlert();
                    }
                });
            }).start();
        } else {
            //Sin conexión a Internet
            botonIngresar.setClickable(true);
            progressDialog.dismiss();
            showConectionAlert();
        }
    }

    public void showUpdateAlert(String versionNameServer){
        // Protección: cerrar el anterior si existe (evita duplicados por requests asíncronos)
        if (currentUpdateAlert != null) {
            try {
                currentUpdateAlert.dismiss();
            } catch (Exception e) {
                Log.w("LoginActivity", "Error al cerrar diálogo anterior: " + e.getMessage());
            }
        }

        currentUpdateAlert = new UpdateAlert();
        currentUpdateAlert.versionNameServer(versionNameServer);
        currentUpdateAlert.show(getSupportFragmentManager(),"Update Alert");
    }

    private void showDeviceErrorAlert(){
        DeviceAlertError myDeviceAlertError = new DeviceAlertError();
        myDeviceAlertError.show(getSupportFragmentManager(),"Device Error");
    }

    private void showInternetConectionAlert(){
        InternetConnectionAlert myInternetConnectionAlert = new InternetConnectionAlert();
        myInternetConnectionAlert.show(getSupportFragmentManager(),"Internet Connection Error");
    }

    private void showConectionAlert(){
        ConnectionAlert myConnectionAlert = new ConnectionAlert();
        myConnectionAlert.show(getSupportFragmentManager(),"Connection Error");
    }

    private void showServerErrorAlert(){
        ServerAlertError myServerAlertError = new ServerAlertError();
        myServerAlertError.show(getSupportFragmentManager(),"Server Error");
    }

    private void serverConnection(String nroLegajo, String clave){
        // Verificar si el servidor esta activo realizando una solicitud
        Thread serverConnectionThread = new Thread(() -> {
            boolean serverConnection = NetworkUtil.isConnectionActive(Configurador.URL_SERVER);
            runOnUiThread(() -> {
                if (serverConnection) {
                    //Conexión al servidor correcta
                    signIn(nroLegajo,clave);
                } else {
                    //Conexión al servidor inactiva
                    botonIngresar.setClickable(true);
                    progressDialog.dismiss();
                    showServerErrorAlert();
                }
            });
        });
        serverConnectionThread.start();
    }
}



