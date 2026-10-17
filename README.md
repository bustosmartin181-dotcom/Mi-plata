# Mi plata

App de Android para administrar la plata día a día. Creada por Martín Enrique Bustos (ver `AUTOR.txt` en el repositorio de GitHub). Versión interna de la app web: **9.9** (`VERSION_APP` en `index.html`).

> Todo lo que dice este documento sale de los archivos reales del proyecto. Lo que no se pudo comprobar está marcado como **(sin comprobar)**. Estado y pruebas: ver `ESTADO_DEL_PROYECTO.md`.

## Qué es y para qué sirve

Mi plata reemplaza las planillas de Excel que usaba el autor para repartir el sueldo del período en un presupuesto diario. Se cargan los ingresos, los gastos importantes (alquiler, servicios, cuotas) y el saldo previo; la app calcula **cuánta plata queda para gastar hoy** y arrastra lo que sobra o falta al día siguiente.

## Funciones que existen en el código

Comprobado leyendo `index.html` (sin probar todas en un teléfono real; ver el estado):

- **Plan:** ingresos por categoría, gastos importantes (con cuotas y estado pagado/pendiente), saldo previo, período con fecha de inicio y fin, metas de ahorro con aportes y cuentas (saldos reales con ajuste).
- **Hoy:** "Te queda para hoy" con semáforo (verde / naranja / rojo), cuánto tendrás mañana, anotar gasto con categorías, lista de gastos (últimos 5, con "Ver todos"), racha de días en verde.
- **Días:** vista día por día del período.
- **🎯 Mi objetivo por día (v9.7):** se elige cuánto gastar por día y se ve cuánto sobra para ahorrar, con tabla de escenarios.
- **Seguridad:** PIN, bloqueo por inactividad y opción de ocultar los montos con puntitos (desde v9.9 es opcional y viene apagada).
- **Apariencia:** tema automático/claro/oscuro y paletas de color.
- **Copias de seguridad:** exportar/recuperar datos en `.json` por el menú compartir, y "Guardar en el teléfono" (carpeta Documentos/MiPlata, v9.8).
- **Exportar:** CSV (días y gastos importantes), **Excel (.xlsx)** y **PDF** hechos a mano en JavaScript, sin librerías (v9.8).
- **🔔 Recordatorio diario (v9.8):** notificación local a la hora elegida.
- **Widget de pantalla de inicio "Mi plata — Hoy" (v9.9):** muestra lo que queda hoy; botón + para anotar un gasto. **Nunca se compiló ni se probó en un teléfono.**
- Existen módulos de sincronización con una cuenta de Claude y de dictado/foto de ticket, pero están **apagados** en esta app (`CONFIG = {"nube": false, "claude": false, "sw": false, "apk": true}`).

## Cómo está hecha

Aplicación **híbrida**: una página web (un solo archivo `index.html` con HTML, CSS y JavaScript, ~2.200 líneas) dentro de un WebView de Android, empaquetada con **Capacitor 6**. No tiene servidor ni base de datos externa. Detalle en `ARQUITECTURA.md`.

## Tecnologías

| Parte | Qué usa | Dónde se ve |
|---|---|---|
| App | HTML + CSS + JavaScript sin frameworks ni bibliotecas | `index.html` |
| Empaquetado | Capacitor `^6.1.0` (`@capacitor/core`, `@capacitor/android`, `@capacitor/cli`) | `package.json` |
| Plugins | `@capacitor/filesystem`, `@capacitor/share`, `@capacitor/local-notifications` (`^6.0.0`) | `package.json` |
| Íconos / pantalla de carga | `@capacitor/assets` `^3.0.5` | `package.json` |
| Widget y MainActivity | Java (Android nativo) | `agregar_widget.py` |
| Compilación | GitHub Actions (Ubuntu, Node 20, Java 17 Temurin, Gradle) | `.github/workflows/construir-apk.yml` |

## Estructura del repositorio de GitHub (lo que se sube)

Todo va **suelto en la raíz**, salvo el workflow:

| Archivo | Para qué sirve |
|---|---|
| `index.html` | **Toda la app.** El workflow la copia a `www/` al compilar. |
| `package.json` | Dependencias de Capacitor y plugins. |
| `capacitor.config.json` | `appId` `com.bustos.miplata`, nombre, `webDir: www`, color de fondo. (Existe en el repo original; no se modificó.) |
| `agregar_widget.py` | Crea los archivos del widget, registra el widget en el AndroidManifest y reemplaza `MainActivity.java`. Lo ejecuta el workflow. |
| `debug.keystore` | Clave de firma fija para que las versiones se instalen una sobre otra. |
| `icon-only.png`, `icon-foreground.png`, `icon-background.png`, `splash.png`, `splash-dark.png` | Fuentes de íconos y pantallas de carga (ícono "barras que suben + moneda"). |
| `.github/workflows/construir-apk.yml` | Receta que genera el APK. |
| `index-2.html`, `LEEME.txt`, `AUTOR.txt` | Archivos viejos / de autoría del repositorio original. `index-2.html` y `LEEME.txt` no los usa la compilación. |
| `probar_nucleo.js` | Pruebas del cálculo (se corre con Node, ver abajo). |

El zip de respaldo `mi-plata-v9_9-codigo.zip` además incluye `www/index.html` (copia de `index.html`), `apk_info.txt`, `AndroidManifest_decoded.txt`, `capacitor.plugins.json`, `config/` y `assets/native-bridge.js`: material extraído del APK 9.6 original, solo de referencia.

## Cómo probar

- **Cálculo y exportación (sin teléfono):** `node probar_nucleo.js` en la carpeta con `index.html`. Prueba el cálculo diario, el objetivo, y que se generen un `.xlsx` y un `.pdf` válidos en estructura.
- **Pantallas:** abrir `index.html` en un navegador. `CONFIG.apk` está en `true`, así que algunas funciones (guardar en el teléfono, recordatorio, widget) solo hacen algo dentro de la app instalada. **(Abrirlo en un navegador no se probó en esta sesión.)**
- **Android:** instalar el APK que genera GitHub Actions.

## Cómo generar el APK (todo desde el teléfono es posible)

1. Subir/reemplazar los archivos de la raíz en GitHub (**Add file → Upload files**) y confirmar con **Commit changes** en `main`.
2. El workflow corre solo (o manualmente en **Actions → Construir APK → Run workflow**).
3. Al terminar con tilde verde, bajar el artefacto `mi-plata-APK` (un zip con `app-debug.apk`) e instalarlo.
4. Versión de Android: `versionCode` = número de ejecución del workflow; `versionName` = `1.0.<número>`. (El APK original 9.6 tenía versionCode 4 / 1.0.4.)

Es un APK **debug** firmado con `debug.keystore`. No es una versión "release" para Play Store.

## Qué hace falta para seguir desarrollando

`index.html`, `package.json`, `capacitor.config.json`, `agregar_widget.py`, `debug.keystore`, los cinco PNG y el workflow. Las instrucciones para continuar están en `INSTRUCCIONES_PARA_OTRA_IA.md`.

## Configuraciones especiales

- `debug.keystore` está dentro del repositorio: si el repositorio es **público**, cualquiera podría firmar una app que se instale como actualización. Conviene repositorio privado.
- Permisos agregados por el workflow al AndroidManifest: `POST_NOTIFICATIONS` y `RECEIVE_BOOT_COMPLETED`.
- Primera vez que se pasa de un APK firmado con otra clave a la clave fija hay que desinstalar la app (se pierden los datos locales si no hay copia de seguridad).
