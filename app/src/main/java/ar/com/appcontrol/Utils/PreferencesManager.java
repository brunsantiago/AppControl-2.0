package ar.com.appcontrol.Utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Gestiona la configuración de empresa usando SharedPreferences
 * Almacena y recupera información sobre la empresa seleccionada durante el provisioning
 */
public class PreferencesManager {

    private static final String PREF_NAME = "AppControlPreferences";
    private static final String KEY_COMPANY_CONFIGURED = "is_company_configured";
    private static final String KEY_COMPANY_ID = "selected_company_id";
    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_COMPANY_LOGO_PATH = "company_logo_local_path";
    private static final String KEY_COMPANY_LOGO_MENU_PATH = "company_logo_menu_local_path";

    private SharedPreferences preferences;

    public PreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Verifica si la empresa ya fue configurada
     * @return true si la empresa está configurada, false en caso contrario
     */
    public boolean isCompanyConfigured() {
        return preferences.getBoolean(KEY_COMPANY_CONFIGURED, false);
    }

    /**
     * Guarda la configuración de empresa
     * @param companyId ID de la empresa (1=SAB-5, 2=CONSISA, 3=BROUCLEAN)
     * @param companyName Nombre de la empresa
     */
    public void saveCompanyConfiguration(int companyId, String companyName) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_COMPANY_CONFIGURED, true);
        editor.putInt(KEY_COMPANY_ID, companyId);
        editor.putString(KEY_COMPANY_NAME, companyName);
        editor.apply();
    }

    /**
     * Guarda la configuración de empresa con logo
     * @param companyId ID de la empresa (1=SAB-5, 2=CONSISA, 3=BROUCLEAN)
     * @param companyName Nombre de la empresa
     * @param logoLocalPath Ruta local donde se guardó el logo
     */
    public void saveCompanyConfiguration(int companyId, String companyName, String logoLocalPath) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_COMPANY_CONFIGURED, true);
        editor.putInt(KEY_COMPANY_ID, companyId);
        editor.putString(KEY_COMPANY_NAME, companyName);
        editor.putString(KEY_COMPANY_LOGO_PATH, logoLocalPath);
        editor.apply();
    }

    /**
     * Guarda la configuración de empresa con ambos logos
     * @param companyId ID de la empresa (1=SAB-5, 2=CONSISA, 3=BROUCLEAN)
     * @param companyName Nombre de la empresa
     * @param logoLocalPath Ruta local donde se guardó el logo principal
     * @param logoMenuLocalPath Ruta local donde se guardó el logo del menú
     */
    public void saveCompanyConfiguration(int companyId, String companyName, String logoLocalPath, String logoMenuLocalPath) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_COMPANY_CONFIGURED, true);
        editor.putInt(KEY_COMPANY_ID, companyId);
        editor.putString(KEY_COMPANY_NAME, companyName);
        editor.putString(KEY_COMPANY_LOGO_PATH, logoLocalPath);
        editor.putString(KEY_COMPANY_LOGO_MENU_PATH, logoMenuLocalPath);
        editor.apply();
    }

    /**
     * Obtiene el ID de la empresa configurada
     * @return ID de la empresa (1, 2, 3) o -1 si no está configurada
     */
    public int getSelectedCompanyId() {
        return preferences.getInt(KEY_COMPANY_ID, -1);
    }

    /**
     * Obtiene el nombre de la empresa configurada
     * @return Nombre de la empresa o null si no está configurada
     */
    public String getSelectedCompanyName() {
        return preferences.getString(KEY_COMPANY_NAME, null);
    }

    /**
     * Obtiene la ruta local del logo de la empresa
     * @return Ruta local del archivo de logo o null si no está configurado
     */
    public String getCompanyLogoPath() {
        return preferences.getString(KEY_COMPANY_LOGO_PATH, null);
    }

    /**
     * Obtiene la ruta local del logo del menú de la empresa
     * @return Ruta local del archivo de logo del menú o null si no está configurado
     */
    public String getCompanyLogoMenuPath() {
        return preferences.getString(KEY_COMPANY_LOGO_MENU_PATH, null);
    }

    /**
     * Limpia la configuración de empresa (útil para testing o reset)
     */
    public void clearCompanyConfiguration() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_COMPANY_CONFIGURED);
        editor.remove(KEY_COMPANY_ID);
        editor.remove(KEY_COMPANY_NAME);
        editor.remove(KEY_COMPANY_LOGO_PATH);
        editor.remove(KEY_COMPANY_LOGO_MENU_PATH);
        editor.apply();
    }
}
