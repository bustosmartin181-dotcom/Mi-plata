# Arquitectura de Mi plata

Todo lo de este documento fue leído en el código real (`index.html`, `agregar_widget.py`, `construir-apk.yml`). Lo que no se pudo comprobar está marcado **(sin comprobar)**.

## Visión general

```
GitHub (archivos sueltos)
   │  push a main
   ▼
GitHub Actions ──► npm install ──► npx cap add android ──► @capacitor/assets (íconos)
   │                                        │
   │  cp index.html → www/                  ▼
   │                                  npx cap sync android
   │                                        │
   │              python3 agregar_widget.py (widget + MainActivity + manifest)
   │              sed (permisos de notificaciones)
   │              sed (versionCode / versionName)
   │              cp debug.keystore → ~/.android/
   ▼                                        ▼
                         ./gradlew assembleDebug  ──►  app-debug.apk (artefacto mi-plata-APK)
```

En el teléfono: Android → `MainActivity` (Capacitor) → **WebView** que carga `index.html` → la app web usa plugins de Capacitor para tocar el sistema.

## Componentes

### 1. La app web: `index.html` (un solo archivo)

Módulos en este orden (están rotulados con comentarios `NN · NOMBRE`):

| Módulo | Qué contiene |
|---|---|
| `CONFIG` (arriba del script) | Interruptores: `{"nube": false, "claude": false, "sw": false, "apk": true}`. `apk: true` habilita lo que depende de Capacitor. |
| 01 · UTILIDADES | Formatos (`pesos`), fechas, `aNumero`, `escapar`. Puras. |
| 02 · NÚCLEO | Las reglas del dinero. Funciones **puras** que reciben el estado `S`: `calcular(S, hoy)`, `diasDelPeriodo`, `semaforo`, `congelarDias`, `objetivoDiario` (v9.7), `rachaEnVerde`, etc. |
| 02b · EXPORTAR | Generadores puros de `.xlsx` (zip sin comprimir + XML a mano) y `.pdf` (texto, Courier/Helvetica, A4): `crearZip`, `xlsxExportar`, `crearPdf`, `pdfExportar`, `datosExportar`. |
| 03 · ESTADO | Estado `S`, `cargar`, `guardarLocal`, `guardar`, `actualizarWidgetResumen`, `migrarEstado`. |
| 04 · SEGURIDAD | PIN (PBKDF2 con sal), bloqueo, `dineroOculto()`, `ACCIONES_PROTEGIDAS`. |
| 05 · NUBE | Sincronización con cuenta de Claude. **Apagada** (`CONFIG.nube=false`). |
| 06 · CLAUDE Y VOZ | Preguntas, dictado y foto de ticket. **Apagado** (`CONFIG.claude=false`). |
| 07 · COMÚN | Avisos, ventanas, tema, `guardarEnDocumentos`, `entregarArchivo`, `programarRecordatorio`. |
| 08 · PANTALLAS | Funciones `tarjeta…`/`pantalla…` que arman el HTML de cada pestaña (Plan, Hoy, Días, Ajustes). Solo leen datos. |
| 09 · ACCIONES | Objeto de acciones (`data-a="..."`) y de cambios de campos (`data-f="..."`), con el despachador que aplica el PIN. |
| 10 · ARRANQUE | Pone la app en marcha: carga, dibuja, engancha eventos, `window.miplataAbrir`. |

#### Teclado y "Anotar gasto" (v9.9.1)
Un par de escuchas globales (`focusin`/`focusout`, justo antes de `window.miplataAbrir`) agregan la clase `teclado` al `body` mientras hay un campo enfocado: el CSS `body.teclado nav, .fab, ::after {display:none}` libera espacio. Si el campo está dentro de `#tarjeta-anotar`, la tarjeta se lleva a la vista con `scrollIntoView({block:'end'})`. Esto supone que el WebView de Android **achica la ventana** al salir el teclado (se vio así en una captura del autor); no se verificó en todos los teléfonos. El botón ＋ (`#fab`) solo se ve fuera de la pestaña Hoy y llama a `abrirAnotarGasto()`.

### 2. Cómo se comunican las partes (dentro de la web)

- **Un solo estado global `S`** (objeto) y un objeto `UI` con cosas temporales (pestaña, borrador, `mostrarDinero`).
- **Dibujo:** `dibujar()` reconstruye el HTML completo a partir de `S`/`UI` y lo pone en `#m`.
- **Eventos:** los botones llevan `data-a="accion"`; los campos `data-f="campo"`. Un despachador central busca la acción en el objeto de acciones (o el campo en el de cambios), la ejecuta, y salvo que devuelva `SIN_GUARDAR`, llama a `guardar()` y `dibujar()`.
- **Flujo típico:** toque → acción cambia `S` → `guardar()` (graba, actualiza el resumen del widget, sube a la nube si está activa) → `dibujar()`.

### 3. Datos y dónde se guardan

- **`localStorage` del WebView** con la clave `miplata_v2` (constante `CLAVE_DATOS`): todo `S` en JSON. Intentos de PIN en `miplata_sec`. Los datos viven **solo en el teléfono**; desinstalar la app los borra.
- Forma de `S` (ver `estadoInicial()`): `sueldo`, `previo`, `bienvenida`, `ingresos[]`, `start`, `end`, `items[]` (gastos importantes), `spent[]` (gastos y aportes a metas), `recal`; además campos agregados con el tiempo: `ajustes`, `metas`, `cuentas`, `pin`, `tema`, `color`, `ocultar`, `objetivo`, `recOn`, `recHora`, `ultimaCopia`, etc. Los datos viejos se adaptan en `migrarEstado()`.
- **Copias de seguridad:** `.json` con todo `S`. "Guardar en la nube" lo entrega por el menú compartir; "Guardar en el teléfono" lo escribe en `Documents/MiPlata/`. "Recuperar" lee un archivo o texto pegado.
- **Resumen del widget:** archivo `widget.json` (ver más abajo). Es una copia derivada, no es fuente de verdad.

### 4. Plugins de Capacitor usados

| Plugin | Uso en el código |
|---|---|
| `Filesystem` | Guardar copias/exportaciones (`DOCUMENTS`, `CACHE`) y `widget.json` (`DATA`). |
| `Share` | Menú compartir para copias, Excel y PDF. |
| `LocalNotifications` | Recordatorio diario (`schedule` con `on: {hour, minute}`, id 7001). |

Se accede por `window.Capacitor.Plugins.<Nombre>`, solo si `CONFIG.apk`.

### 5. El widget de Android (v9.9) — **sin probar en dispositivo**

Todo se instala al compilar con `agregar_widget.py` (que contiene los archivos como texto):

- `res/layout/widget_miplata.xml`, `res/xml/widget_miplata_info.xml`, `res/values(-night)/colors_widget.xml`, `res/values/strings_widget.xml`, `res/drawable/*` (fondo, botón, ícono +, vista previa).
- `MiPlataWidgetProvider.java` (`AppWidgetProvider`): lee `context.getFilesDir()/widget.json` y arma la vista. Colores verde/naranja/rojo según `color`; "••••••" si `oculto`; "Dato de otro día" si `fecha` no es hoy; línea de objetivo si `objetivo > 0`.
- `MainActivity.java` (**reemplaza** el de Capacitor): extiende `BridgeActivity`. Si llega el extra `miplata_action=anotar` (botón +), ejecuta `window.miplataAbrir('anotar', id)` en el WebView a los 300, 1200 y 2800 ms (la página puede tardar en cargar). La página ignora repeticiones del mismo `id`. Además refresca el widget en `onResume` y `onPause`.
- AndroidManifest: `<receiver ...MiPlataWidgetProvider>` con `APPWIDGET_UPDATE` y `android.appwidget.provider`.
- **Contrato JS ↔ Java (`widget.json`):** `quedaHoy`, `gastoHoy`, `saldo`, `color` (`ok|warn|neg`), `oculto`, `objetivo`, `objetivoSobra`, `fecha` (`AAAA-MM-DD`, fecha local), `upd`, `version`. El JS lo escribe (con retraso de 400 ms) en cada `guardar()` y al arrancar.
- Actualización: al salir/entrar a la app, y por `updatePeriodMillis` = 30 minutos (mínimo de Android).

### 6. Permisos y configuración

- AndroidManifest (por el workflow): `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`; el resto lo genera Capacitor. `minSdk 22`, `targetSdk 34` según `apk_info.txt` del APK 9.6 original.
- Sin permiso de almacenamiento: "Guardar en el teléfono" depende de que Android permita escribir en Documentos **(sin comprobar en dispositivos reales)**.
- Sin servicios externos ni red: la app funciona sin internet. No hay API ni servidor.

## Partes que hay que tocar con cuidado

1. **`calcular()` y el módulo 02:** son el corazón; muchas pantallas, el objetivo, la exportación y el widget dependen de sus campos (`quedaHoy`, `gastoHoy`, `saldo`, `baseHoy`, `faltan`, `hoyIdx`…). No cambiar nombres sin buscar todos los usos.
2. **`CLAVE_DATOS` y `migrarEstado()`:** si se cambia el formato de `S`, hay que migrar los datos viejos. Si no, los usuarios pierden o rompen sus datos.
3. **Marcas del archivo:** `probar_nucleo.js` busca el texto `const MS_DIA` y el rótulo `03 · ESTADO`. No renombrarlos sin actualizar la prueba. El bloque 02b debe quedar **antes** de 03 y sin depender del DOM.
4. **`const VERSION_APP` antes de `const VERSION`:** el PDF usa `VERSION_APP`, que se define dentro del bloque puro.
5. **`ACCIONES_PROTEGIDAS`:** toda acción nueva que muestre datos o exporte debería estar ahí si la app tiene PIN.
6. **Contrato de `widget.json`:** si se cambia una clave en el JS hay que cambiarla en `MiPlataWidgetProvider.java` (dentro de `agregar_widget.py`) y viceversa.
7. **`agregar_widget.py` reemplaza `MainActivity.java`:** si en el futuro se agrega código nativo propio a `MainActivity`, hay que incorporarlo ahí.
8. **Clave de firma:** cambiar o perder `debug.keystore` obliga a todos los usuarios a desinstalar para actualizar.
9. **Orden de los pasos del workflow:** `cap sync` → `agregar_widget.py` → permisos → numerar versión → clave → `gradlew`.
10. **Textos dentro de `agregar_widget.py`:** los archivos están en cadenas crudas `r'''...'''`; no puede aparecer `'''` dentro del contenido. El Java usa escapes `\uXXXX` para acentos.
