package ar.com.appcontrol.POJO;

/**
 * Modelo de datos para empresas disponibles en el sistema
 */
public class Company {

    private int id;
    private String name;
    private String displayName;
    private String logoStoragePath; // Ruta en Firebase Storage para logo principal
    private String logoMenuStoragePath; // Ruta en Firebase Storage para logo del menú

    public Company(int id, String name, String displayName, String logoStoragePath, String logoMenuStoragePath) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.logoStoragePath = logoStoragePath;
        this.logoMenuStoragePath = logoMenuStoragePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getLogoStoragePath() {
        return logoStoragePath;
    }

    public void setLogoStoragePath(String logoStoragePath) {
        this.logoStoragePath = logoStoragePath;
    }

    public String getLogoMenuStoragePath() {
        return logoMenuStoragePath;
    }

    public void setLogoMenuStoragePath(String logoMenuStoragePath) {
        this.logoMenuStoragePath = logoMenuStoragePath;
    }

    /**
     * Retorna las empresas disponibles en el sistema
     * SAB-5 (1), CONSISA (2), BROUCLEAN (3)
     * Las rutas de logos son relativas a Firebase Storage
     */
    public static Company[] getAvailableCompanies() {
        return new Company[] {
            new Company(1, "SAB-5", "SAB-5",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/SAB-5/logos/logo.png",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/SAB-5/logos/logo_menu.png"),
            new Company(2, "CONSISA", "CONSISA",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/CONSISA/logos/logo.png",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/CONSISA/logos/logo_menu.png"),
            new Company(3, "BROUCLEAN", "BROUCLEAN",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/BROUCLEAN/logos/logo.png",
                "accounts/SB9mPqR7sLwN2vH8Tz3F/entities/BROUCLEAN/logos/logo_menu.png")
        };
    }

    /**
     * Busca una empresa por ID
     * @param id ID de la empresa
     * @return Objeto Company o null si no existe
     */
    public static Company getById(int id) {
        for (Company company : getAvailableCompanies()) {
            if (company.getId() == id) {
                return company;
            }
        }
        return null;
    }

    /**
     * Busca una empresa por nombre
     * @param name Nombre de la empresa (SAB-5, CONSISA, BROUCLEAN)
     * @return Objeto Company o null si no existe
     */
    public static Company getByName(String name) {
        for (Company company : getAvailableCompanies()) {
            if (company.getName().equalsIgnoreCase(name)) {
                return company;
            }
        }
        return null;
    }
}
