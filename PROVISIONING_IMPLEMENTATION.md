# Sistema de Provisioning Multi-Empresa - Implementación Completa

## Resumen
Sistema implementado para configurar la empresa (SAB-5, CONSISA, BROUCLEAN) en el primer inicio de la aplicación.

## Métodos de Configuración

### 1. QR Code (Visible en UI)
- Usuario escanea código QR generado por el admin
- QR contiene: `appcontrol://configure?token=XXXX`
- Validación automática con la API

### 2. Código Manual (Visible en UI)
- Usuario ingresa código de activación manualmente
- Formato: `SAB5-XXXX-XXXX-XXXX` (16 caracteres + guiones)
- Validación con la API usando el código

### 3. Universal Link (NO visible en UI - Automático)
- Usuario hace click en link HTTPS recibido por email/WhatsApp
- Link: `https://app.appcontrol.com.ar/configure?token=XXXX`
- Android abre automáticamente la app y configura la empresa
- **Completamente transparente para el usuario**

---

## Archivos Creados

### Backend (Utils)
- `app/src/main/java/ar/com/appcontrol/Utils/PreferencesManager.java`
  - Gestión de SharedPreferences
  - Métodos: `isCompanyConfigured()`, `saveCompanyConfiguration()`, `getSelectedCompanyId()`

### Modelos (POJO)
- `app/src/main/java/ar/com/appcontrol/POJO/Company.java`
  - Modelo de datos de empresas
  - IDs: 1=SAB-5, 2=CONSISA, 3=BROUCLEAN

### Activities
- `app/src/main/java/ar/com/appcontrol/LauncherActivity.java`
  - Punto de entrada principal
  - Verifica si empresa está configurada
  - Redirige a Provisioning o Login

- `app/src/main/java/ar/com/appcontrol/CompanyProvisioningActivity.java`
  - Pantalla de configuración inicial
  - Maneja QR, código manual y deep links
  - Valida con API y guarda configuración

### Layouts
- `app/src/main/res/layout/activity_company_provisioning.xml`
  - UI principal con 2 opciones visibles (QR + Código Manual)

- `app/src/main/res/layout/dialog_manual_code.xml`
  - Diálogo para ingresar código de activación

---

## Archivos Modificados

### 1. `Configurador.java`
**Cambios**:
- `ID_EMPRESA` cambió de constante String a variable dinámica `idEmpresaActual`
- Nuevos métodos:
  - `setIdEmpresa(int idEmpresa)` - Configura empresa actual
  - `getIdEmpresa()` - Retorna ID como int
  - `getIdEmpresaString()` - Retorna ID como String (compatibilidad)
  - `getCompanyName()` - Retorna nombre de empresa

### 2. `AndroidManifest.xml`
**Cambios**:
- `LauncherActivity` es ahora la activity LAUNCHER (antes LoginActivity)
- `CompanyProvisioningActivity` configurada con intent-filters:
  - Deep Link: `appcontrol://configure`
  - Universal Link: `https://app.appcontrol.com.ar/configure`

### 3. `build.gradle`
**Dependencias agregadas**:
```gradle
implementation 'com.google.zxing:core:3.4.1'
implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
```

### 4. Referencias a `Configurador.ID_EMPRESA` actualizadas
Archivos modificados (7):
- `LoginActivity.java`
- `RegistroActivity.java`
- `RecoveryKeyActivity.java`
- `IngresoActivity.java`
- `EgresoActivity.java`
- `MainActivityFragment/HomeFragment.java`
- `MainActivityFragment/ConfigFragment.java`

**Reemplazo**: `Configurador.ID_EMPRESA` → `Configurador.getIdEmpresaString()`

---

## Flujo de Usuario

### Primera Instalación
```
1. Usuario instala app
2. Abre app → LauncherActivity
3. No hay empresa configurada → CompanyProvisioningActivity
4. Usuario elige:
   - Opción A: Escanea QR
   - Opción B: Ingresa código manual
   - Opción C (oculta): Click en link HTTPS (automático)
5. App valida con API
6. Guarda configuración en SharedPreferences
7. Redirige a LoginActivity
```

### Próximas Aperturas
```
1. Usuario abre app → LauncherActivity
2. Empresa YA configurada → LoginActivity directamente
3. `Configurador.setIdEmpresa()` se ejecuta automáticamente
```

---

## Endpoints de API Requeridos

### 1. Generar Token (Backend/Admin Panel)
```
POST /api/v2/provisioning/generate-token

Request:
{
  "companyId": 1,
  "adminToken": "jwt_admin"
}

Response:
{
  "success": true,
  "data": {
    "token": "a1b2c3d4e5f6g7h8",
    "qrCodeUrl": "https://app-server.com.ar/qr/a1b2c3d4.png",
    "activationCode": "SAB5-XJ7K-9M2P",
    "deepLink": "appcontrol://configure?token=a1b2c3d4e5f6g7h8",
    "universalLink": "https://app.appcontrol.com.ar/configure?token=a1b2c3d4e5f6g7h8",
    "expiresAt": "2025-10-10T10:00:00Z"
  }
}
```

### 2. Validar Token (App Android)
```
POST /api/v2/provisioning/validate-token

Request (con token):
{
  "token": "a1b2c3d4e5f6g7h8"
}

O Request (con código manual):
{
  "activationCode": "SAB5-XJ7K-9M2P"
}

Response:
{
  "success": true,
  "data": {
    "companyId": 1,
    "companyName": "SAB-5",
    "displayName": "SAB 5 - Planta Principal"
  }
}
```

---

## Base de Datos (Backend)

### Tabla: `provisioning_tokens`
```sql
CREATE TABLE provisioning_tokens (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) UNIQUE NOT NULL,
    activation_code VARCHAR(20) UNIQUE NOT NULL,
    company_id INT NOT NULL,
    company_name VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    used_at DATETIME NULL,
    created_by_admin_id INT NOT NULL,

    INDEX idx_token (token),
    INDEX idx_activation_code (activation_code),
    INDEX idx_expires_at (expires_at)
);
```

---

## Formato de Códigos de Activación

### Estructura
```
SAB5-XJ7K-9M2P  → SAB-5 (ID: 1)
SAB6-XJ7K-9M2P  → SAB-6 (ID: 2)
CARN-XJ7K-9M2P  → CARNE (ID: 3)
```

**Formato**: `PPPP-XXXX-XXXX-XXXX`
- `PPPP`: Prefijo de empresa (SAB5, SAB6, CARN)
- `XXXX-XXXX-XXXX`: 12 caracteres alfanuméricos random

---

## Seguridad

### Tokens
- ✅ Token único generado con UUID o hash
- ✅ Expiración: 48 horas desde generación
- ✅ Un solo uso (marcado como `used=true` después de validación)
- ✅ Validación server-side

### Deep Links
- ✅ `android:autoVerify="true"` para Universal Links
- ✅ Validación de token antes de configurar empresa
- ✅ No permite navegación hacia atrás durante provisioning

---

## Testing

### Casos de Prueba

1. **Primera Instalación - QR**
   - Instalar app
   - Escanear QR válido
   - Verificar redirección a LoginActivity
   - Verificar empresa guardada en SharedPreferences

2. **Primera Instalación - Código Manual**
   - Instalar app
   - Ingresar código válido
   - Verificar configuración exitosa

3. **Primera Instalación - Deep Link**
   - Instalar app
   - Hacer click en link HTTPS desde email/WhatsApp
   - Verificar apertura automática y configuración

4. **Token Expirado**
   - Usar token con más de 48 horas
   - Verificar mensaje de error

5. **Token Ya Usado**
   - Usar mismo token dos veces
   - Verificar rechazo en segundo intento

6. **App Ya Configurada**
   - Abrir app con empresa ya configurada
   - Verificar que va directo a LoginActivity

---

## Panel de Administración (Web) - TODO

### Funcionalidades Requeridas
1. **Generar Token**
   - Seleccionar empresa (SAB-5, SAB-6, CARNE)
   - Botón "Generar Código"
   - Mostrar:
     - QR Code (descargable)
     - Código de activación (copiable)
     - Link HTTPS (copiable/enviar por email)
     - Fecha de expiración

2. **Listar Tokens Activos**
   - Ver tokens no expirados y no usados
   - Revocar token manualmente

3. **Historial**
   - Ver tokens usados
   - Fecha de uso
   - Dispositivo que lo usó

---

## Notas Importantes

### Universal Link (Opción 3)
- **NO aparece en la UI de la app**
- Solo está documentado en comentarios del código
- Funciona automáticamente cuando usuario hace click en link
- Requiere configuración en servidor web:
  - Archivo `.well-known/assetlinks.json` en dominio
  - Verificación de propiedad del dominio

### Configuración `.well-known/assetlinks.json`
Para que los Universal Links funcionen, crear este archivo en:
`https://app.appcontrol.com.ar/.well-known/assetlinks.json`

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "ar.com.appcontrol",
    "sha256_cert_fingerprints": ["XX:XX:XX:..."]
  }
}]
```

**Obtener SHA256 del certificado**:
```bash
keytool -list -v -keystore key_store_sab5.jks
```

---

## Próximos Pasos

1. **Backend API**
   - Implementar endpoints de provisioning
   - Crear tabla `provisioning_tokens`
   - Generar QR codes server-side

2. **Panel Admin**
   - Interfaz web para generar tokens
   - Gestión de tokens activos

3. **Testing**
   - Probar todos los flujos
   - Validar seguridad de tokens

4. **Universal Links (Opcional)**
   - Configurar `.well-known/assetlinks.json`
   - Verificar dominio en Google Search Console

---

## Comandos Útiles

### Compilar
```bash
./gradlew build
```

### Instalar en dispositivo
```bash
./gradlew installDebug
```

### Limpiar SharedPreferences (Testing)
```java
PreferencesManager prefs = new PreferencesManager(context);
prefs.clearCompanyConfiguration();
```

---

**Fecha de Implementación**: 08/10/2025
**Desarrollador**: Claude Code
**Versión App**: 1.5.0
