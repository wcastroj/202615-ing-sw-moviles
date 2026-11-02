# Desarrollo de ingeniería de software para aplicaciones móviles  — Grupo 08
 
Este repositorio es el espacio de trabajo del **Grupo 08** para el proyecto del curso **Ingeniería de software para aplicaciones móviles**.
 
 
## Integrantes
 
| Nombre                              | Correo                       |
|-------------------------------------|------------------------------|
| Fabian Santiago Quiroga cubides     | f.quirogac@uniandes.edu.co   |
| William Marrion Castro Jiménez      | w.satroj@uniandes.edu.co     |
| Juan Sebastian Chimbaco Bonilla     | j.chimbaco@uniandes.edu.co   |
| Joaquin Santiago Salcedo Montenegro | j.salcedom23@uniandes.edu.co |
 
---
 
## Construir la aplicación de forma local
 
### Requisitos
 
| Herramienta | Versión |
|---|---|
| Android Studio | Versión reciente compatible con AGP 8.13 |
| JDK para Gradle | **21** (no usar JDK 25: Gradle 8.14 no lo soporta) |
| Android SDK | Platform 34 (compileSdk / targetSdk 34, minSdk 21) |
| Gradle | 8.14.6 (se descarga solo con el wrapper `gradlew`) |
| Dispositivo | Emulador o teléfono con Android 5.0 (API 21) o superior |
 
### 1. Clonar el repositorio
 
```bash
git clone https://github.com/wcastroj/202615-ing-sw-moviles.git
cd 202615-ing-sw-moviles
```
 
### 2. Abrir el proyecto en Android Studio
 
1. **File → Open** y seleccionar la carpeta del repositorio.
2. Configurar el JDK de Gradle en **File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** y elegir un **JDK 21** (por ejemplo `jbr-21`). Si no aparece, usar **Download JDK…** o **Add JDK…**.
> Si Android Studio muestra *"Invalid Gradle JDK configuration found"*, no elegir *Use Embedded JDK* (es JDK 25); elegir el JDK 21.
3. Hacer **Sync Project with Gradle Files**.
 
### 3. Compilar el APK
 
Desde Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
 
Desde la terminal (con `JAVA_HOME` apuntando a un JDK 21):
 
```bash
# macOS / Linux
./gradlew assembleDebug
```
 
```powershell
# Windows (PowerShell)
$env:JAVA_HOME = "C:\ruta\a\jdk-21"; .\gradlew.bat assembleDebug
```
 
El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.
 
### 4. Ejecutar la aplicación
 
- Desde Android Studio: elegir el emulador o dispositivo y pulsar **Run ▶**.
- O instalar el APK con `adb`:
 
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
 
### Backend
 
La app consume el backend de Vinyls en `https://backvynils-q6yc.onrender.com/` (constante `BASE_URL` en `app/src/main/java/com/example/vinyls_application/network/NetworkServiceAdapter.kt`).
 
- Render "duerme" el backend cuando no se usa: si la primera carga falla, abrir https://backvynils-q6yc.onrender.com/albums en el navegador, esperar a que responda y volver a intentar.
- Para usar un backend local (por ejemplo en el puerto 3000), cambiar temporalmente `BASE_URL` a `http://10.0.2.2:3000/` (en el emulador, `10.0.2.2` es el `localhost` del PC). No subir ese cambio al repositorio.
 
### Pruebas
 
```bash
# Pruebas unitarias (JUnit, Mockito, Robolectric)
./gradlew testDebugUnitTest
 
# Pruebas E2E con Espresso (requiere un emulador o dispositivo conectado)
./gradlew connectedDebugAndroidTest
 
# Pruebas E2E de compatibilidad en Android 13 (API 33) y Android 15 (API 35) con emuladores administrados por Gradle
./gradlew compatibilidadGroupDebugAndroidTest
```
 
Reportes:
 
- Unitarias: `app/build/reports/tests/testDebugUnitTest/index.html`
- E2E: `app/build/reports/androidTests/connected/debug/index.html`
