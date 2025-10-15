
package ar.com.appcontrol;

import java.util.Date;

public class Configurador {

    public static final String API_PATH = "http://192.168.68.50:3000/api/v2/"; // PRUEBAS LOCALES PC NUEVA

    //public static final String API_PATH = "https://app-server.com.ar/api/v2/"; // PLESK - V2 JWT - app-server.com.ar

    //public static final String API_PATH = "https://api-dm.app-server.com.ar/api/"; // PLESK - api-dm.app-server.com.ar

    // ID_EMPRESA ahora es dinámico, configurado en el provisioning
    // SAB-5 = 1 - CONSISA = 2 - BROUCLEAN = 3
    private static int idEmpresaActual = 1; // Default SAB-5 por compatibilidad

    public static final String URL_SERVER = "https://app-server.com.ar";

    private Date finSesion;

    private static Configurador miconfigurador;

    public  static Configurador getInstance() {
        if (miconfigurador==null) {
            miconfigurador = new Configurador();
        }
        return miconfigurador;
    }

    private Configurador(){ }

    public Date getFinSesion() {
        return finSesion;
    }

    public void setFinSesion(Date finSesion) {
        this.finSesion = finSesion;
    }

    /**
     * Configura el ID de empresa actual
     * @param idEmpresa ID de la empresa (1=SAB-5, 2=CONSISA, 3=BROUCLEAN)
     */
    public static void setIdEmpresa(int idEmpresa) {
        if (idEmpresa >= 1 && idEmpresa <= 3) {
            idEmpresaActual = idEmpresa;
        } else {
            throw new IllegalArgumentException("ID de empresa inválido: " + idEmpresa);
        }
    }

    /**
     * Obtiene el ID de empresa actual
     * @return ID de la empresa configurada
     */
    public static int getIdEmpresa() {
        return idEmpresaActual;
    }

    /**
     * Obtiene el ID de empresa como String (para compatibilidad con código legacy)
     * @return ID de la empresa como String
     */
    public static String getIdEmpresaString() {
        return String.valueOf(idEmpresaActual);
    }

    /**
     * Obtiene el nombre de la empresa actual
     * @return Nombre de la empresa (SAB-5, CONSISA, BROUCLEAN)
     */
    public static String getCompanyName() {
        switch (idEmpresaActual) {
            case 1:
                return "SAB-5";
            case 2:
                return "CONSISA";
            case 3:
                return "BROUCLEAN";
            default:
                return "DESCONOCIDA";
        }
    }
}
