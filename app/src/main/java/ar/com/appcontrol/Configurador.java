
package ar.com.appcontrol;

import java.util.Date;

public class Configurador {

    //public static final String API_PATH = "http://192.168.68.56:3000/api/v2/"; // PRUEBAS LOCALES PC NUEVA

    public static final String API_PATH = "https://app-server.com.ar/api/v2/"; // PLESK - V2 JWT - app-server.com.ar

    //public static final String API_PATH = "https://api-dm.app-server.com.ar/api/"; // PLESK - api-dm.app-server.com.ar

    public static final String ID_EMPRESA = "1"; // SAB-5 = 1 - CONSISA = 2 - BROUCLEAN = 3

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
}
