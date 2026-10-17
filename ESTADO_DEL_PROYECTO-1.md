# Estado del proyecto — fin de la sesión del 2026-10-16

Versión del código: **9.9.1** (`VERSION_APP` en `index.html`). Base: código extraído del APK **9.6** que subió el autor.

## Resumen honesto

| Parte | Estado |
|---|---|
| Cálculo del presupuesto diario (núcleo) | Funciona en pruebas de Node. Es código que el autor ya usaba en el APK 9.6. |
| Objetivo diario (v9.7) | Probado en Node. **Además el autor mandó una captura** de la tarjeta funcionando en su teléfono (una versión 9.7/9.8; no se verificó cuál). |
| Exportar Excel y PDF (v9.8) | Archivos de prueba generados y abiertos con openpyxl y LibreOffice (xlsx), qpdf y pdftotext (pdf). **No se probó en Android** (guardado + menú compartir). |
| Guardar en el teléfono (v9.8) | **Sin probar en ningún teléfono.** Depende de que Android permita escribir en Documentos. |
| Recordatorio diario (v9.8) | **Sin probar.** Plugin y permisos agregados por el workflow; nunca se vio sonar una notificación. |
| Números visibles por defecto (v9.9) | Cambio de 3 líneas; sintaxis comprobada; **sin probar en pantalla**. |
| Reordenado de Hoy/Días y teclado (v9.9.1) | **Probado en un navegador real** (Chromium sin ventana, teclado simulado con la ventana achicada): orden de tarjetas, botón azul visible junto al monto, barra de abajo que se esconde y vuelve, y anotar un gasto. **No probado con el teclado real de Android.** |
| Widget "Mi plata — Hoy" (v9.9) | **No terminado: nunca se compiló con Android real ni se instaló.** Ver abajo. |
| Ícono nuevo (v9.9) | Imágenes generadas y revisadas visualmente; **no se vio en un teléfono**. |

## Qué funciona y fue probado (en esta sesión)

Ejecutado en el entorno de la sesión (sin Android ni red):

1. `node --check` sobre el JavaScript de `index.html`: sin errores de sintaxis.
2. `node probar_nucleo.js`: cálculo diario, objetivo diario (con y sin objetivo) y generación de `.xlsx` y `.pdf` con la firma correcta (`PK` / `%PDF-`).
3. El `.xlsx` generado se abre con openpyxl y con LibreOffice; el `.pdf` pasa `qpdf --check` y `pdftotext` lee el texto (con acentos y ñ).
4. Simulación de `agregar_widget.py` sobre un proyecto Android falso (con `MainActivity.java` y `AndroidManifest.xml` de ejemplo): escribe los 11 archivos, registra el receiver una sola vez (se corrió dos veces) y deja los 10 XML bien formados.
5. El Java del widget y de `MainActivity` **compila contra clases de reemplazo (stubs)** escritas a mano que imitan la API de Android y Capacitor. Esto detecta errores de sintaxis y de tipos propios, pero **no es una compilación real**.
6. `construir-apk.yml` es YAML válido y los pasos están en el orden esperado.
7. Comprobado con una simulación que la función `actualizarWidgetResumen()` escribe `widget.json` con las claves que lee el Java (`quedaHoy`, `gastoHoy`, `color`, `oculto`, `objetivo`, `objetivoSobra`, `fecha`).

## Qué NO se probó

- Ninguna compilación en GitHub Actions de las versiones 9.8 y 9.9 (se desconoce si pasan).
- Ninguna ejecución en un teléfono por parte de la IA. (Desde la v9.9.1 sí hay prueba en navegador de escritorio con `probar_pantallas.py`; el comportamiento real del teclado de Android puede diferir.)
- Instalar el APK nuevo sobre uno viejo (actualización con la clave fija).
- Widget: agregarlo a la pantalla de inicio, valores mostrados, botón +, modo oscuro, aviso de dato viejo, actualización al salir de la app, comportamiento con `updatePeriodMillis` (30 min).
- Notificación diaria, permiso de notificaciones en Android 13+.
- Guardar en Documentos/MiPlata en distintas versiones de Android.
- Compartir/guardar el Excel y el PDF desde la app.

## Errores y riesgos conocidos

- **Primera instalación con la clave fija:** un APK firmado con otra clave obliga a desinstalar antes (se pierden los datos si no hay copia). Les va a pasar al autor, a su esposa y a su compañero (que tiene la 9.6).
- **`debug.keystore` en el repositorio:** riesgo de seguridad si el repo es público.
- El recordatorio suena todos los días aunque ya se hayan anotado gastos y puede atrasarse unos minutos.
- El PDF es de texto simple (sin gráficos).
- Se supone, sin haberlo verificado en el código del plugin, que `Directory.DATA` de Capacitor Filesystem en Android equivale a `context.getFilesDir()`, que es donde el widget busca `widget.json`. **(Sin comprobar.)**
- Se supone que el proyecto que crea `npx cap add android` con Capacitor 6 no compila Kotlin, por eso el widget está en Java. **(Sin comprobar contra un proyecto real.)**
- Se supone que `MainActivity.java` generado por Capacitor es una clase vacía en el paquete `com.bustos.miplata`; `agregar_widget.py` la **reemplaza completa** y falla con error si no encuentra `MainActivity.java`.
- Guardar en el teléfono puede fallar en Android 9 o anterior (falta el permiso de almacenamiento, que no se agregó al manifiesto) y se desconoce el comportamiento exacto en Android 11+.

## Qué falta para completar el widget y comprobar que funciona

1. Subir los archivos y correr el workflow; si falla, leer el error del paso "Agregar el widget" o "Construir APK".
2. Instalar el APK, mantener apretada la pantalla de inicio → Widgets → "Mi plata — Hoy".
3. Abrir la app, anotar un gasto, salir: el monto del widget debe cambiar.
4. Tocar el botón **+** con la app cerrada y con la app abierta: debe abrir la pestaña Hoy con el cursor en "Monto".
5. Probar con "Ocultar los montos" activado (debe mostrar puntitos) y con modo oscuro.
6. Probar al día siguiente sin abrir la app: debe decir "Dato de otro día · abrí la app".

## Problemas que no se resolvieron y por qué

- No se pudo compilar un APK real: el entorno de la sesión no tiene Android SDK ni acceso a red.
- No se pudo ver el widget ni la notificación: no hay dispositivo ni emulador.

## Tareas pendientes (del backlog, **no implementadas**)

Ideas conversadas con el autor, sin código: gráficos por categoría, comparar meses, gastos fijos automáticos, cuentas en dólares, presupuesto por categoría ("10.0"); inicio de sesión con usuarios, sincronización entre teléfonos y presupuesto familiar compartido ("Mi plata Pro", requiere servidor); firma `release` y Play Store; botón para pasar lo ahorrado a una meta.

## Archivos creados, modificados y eliminados en esta sesión

Ver `HISTORIAL_CAMBIOS.md`.
