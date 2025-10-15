package ar.com.appcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ar.com.appcontrol.AlertDialog.ExitAlert;
import ar.com.appcontrol.AlertDialog.LogOutAlert;
import ar.com.appcontrol.Utils.PreferencesManager;
import com.google.android.material.navigation.NavigationView;

import java.io.File;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String ESTADO_SESION = "es";
    private AppBarConfiguration mAppBarConfiguration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_config, R.id.nav_exit)
                .setDrawerLayout(drawer)
                .build();


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.getMenu().getItem(0).setChecked(true);

        Menu menu = navigationView.getMenu();
        MenuItem titleNav = menu.findItem(R.id.titleNav);
        MenuItem titleDisp = menu.findItem(R.id.titleDisp);
        SpannableString sTitleNav = new SpannableString(titleNav.getTitle());
        SpannableString sTitleDisp = new SpannableString(titleDisp.getTitle());
        sTitleNav.setSpan(new TextAppearanceSpan(this, R.style.NavigationDrawerTitle), 0, sTitleNav.length(), 0);
        sTitleDisp.setSpan(new TextAppearanceSpan(this, R.style.NavigationDrawerTitle), 0, sTitleDisp.length(), 0);
        titleNav.setTitle(sTitleNav);
        titleDisp.setTitle(sTitleDisp);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_exit) {
                    SharedPreferences prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE);
                    boolean estadoSesion = prefs.getBoolean(ESTADO_SESION,false);
                    if(estadoSesion){
                        showLogOutAlert();
                    } else {
                        showExitAlert();
                    }
                } else {
                    NavigationUI.onNavDestinationSelected(item, navController);
                    drawer.closeDrawers();
                }
                return false;
            }
        });

        // Cargar el logo de la empresa desde PreferencesManager
        loadCompanyLogo();

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void cambiarItemMenu(){
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);
    }

    public void showLogOutAlert(){
        LogOutAlert myAlert = new LogOutAlert();
        myAlert.show(getSupportFragmentManager(),"Log Out Alert");
    }

    public void showExitAlert(){
        ExitAlert myAlert = new ExitAlert();
        myAlert.show(getSupportFragmentManager(),"Exit Alert");
    }

    public void setNavigationHeaderData(String nombre,String objetivo){
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView navNombre = (TextView) headerView.findViewById(R.id.textViewNombre);
        navNombre.setText(nombre);
        TextView navObjetivo = (TextView) headerView.findViewById(R.id.textViewObjetivo);
        navObjetivo.setText(objetivo);
        navObjetivo.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.gris));
    }

    private void loadCompanyLogo() {
        PreferencesManager preferencesManager = new PreferencesManager(this);
        String logoMenuPath = preferencesManager.getCompanyLogoMenuPath();

        // Si no hay logo del menú, intentar con el logo principal como fallback
        if (logoMenuPath == null || logoMenuPath.isEmpty()) {
            logoMenuPath = preferencesManager.getCompanyLogoPath();
        }

        if (logoMenuPath != null && !logoMenuPath.isEmpty()) {
            File logoFile = new File(logoMenuPath);
            if (logoFile.exists()) {
                Bitmap logoBitmap = BitmapFactory.decodeFile(logoMenuPath);
                if (logoBitmap != null) {
                    NavigationView navigationView = findViewById(R.id.nav_view);
                    View headerView = navigationView.getHeaderView(0);
                    ImageView logoImageView = headerView.findViewById(R.id.imageView);
                    logoImageView.setImageBitmap(logoBitmap);
                }
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        return false;
    }

}