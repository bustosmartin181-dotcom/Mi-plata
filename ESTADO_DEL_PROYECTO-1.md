# Estado del proyecto — fin de la sesión del 2026-10-16

Versión del código: **9.9.2** (`VERSION_APP` en `index.html`). Base: código extraído del APK **9.6** que subió el autor.

## Resumen honesto

| Parte | Estado |
|---|---|
| Cálculo del presupuesto diario (núcleo) | Funciona en pruebas de Node. Es código que el autor ya usaba en el APK 9.6. |
| Objetivo diario (v9.7) | Probado en Node. **Además el autor mandó una captura** de la tarjeta funcionando en su teléfono (una versión 9.7/9.8; no se verificó cuál). |
| Exportar Excel y PDF (v9.8) | Archivos de prueba generados y abiertos con openpyxl y LibreOffice (xlsx), qpdf y pdftotext (pdf). **No se probó en Android** (guardado + menú compartir). |
| Guardar en el teléfono (v9.8) | **Sin probar en ningún teléfono.** Depende de que Android permita escribir en Documentos. |
| Recordatorio diario (v9.8) | **Sin probar.** Plugin y permisos agregados por el workflow; nunca se vio sonar una notificación. |
| Números visibles por defecto (v9.9) | Cambio de 3 líneas; sintaxis comprobada; **sin probar en pantalla**. |
| Objetivo como límite oficial (v9.9.2) | **Probado en navegador real** (`probar_pantallas.py`, 13 comprobaciones) y en Node (`probar_nucleo.js`): escribir un número + Enter cambia "Te queda para hoy" y "Mañana tendrás"; borrarlo vuelve a lo normal; "Recalibrar" saca el objetivo. **No probado en el teléfono.** |
| Reordenado de Hoy/Días y teclado (v9.9.1) | **Probado en un navegador real** (Chromium sin ventana, teclado simulado). **Además el autor, tras instalar esta versión, escribió "ahora sí anda todo"** (reporte suyo, general; no detalló punto por punto). |
| Widget "Mi plata — Hoy" (v9.9) | **El autor informó que compiló, se instaló y que el widget apareció "perfecto" en su teléfono** (reporte suyo, no verificado por la IA). **Sin confirmar:** que muestre los valores correctos, el botón +, el modo oscuro y la actualización. Ver abajo. |
| Ícono nuevo (v9.9) | Imágenes generadas y revisadas visualmente; **no se vio en un teléfono**. |

## Confirmado por el autor en su teléfono (reportes del autor; la IA no pudo verificarlos)

- **v9.9:** la compilación en GitHub Actions con el widget funcionó (se instaló un APK con el widget) y el widget apareció en el selector de widgets y se veía "perfecto". Esto también indica que `agregar_widget.py` y el Java del widget compilaron de verdad.
- **v9.9.1:** "ahora sí anda todo": objetivo en Días, "Anotar gasto" debajo de "Te queda para hoy" y botón azul visible con el teclado. El autor dijo que le encantó "Mi objetivo por día".
- **v9.9.2:** todavía sin reporte.
- **Sin ningún reporte:** recordatorio diario, Excel, PDF, "Guardar en el teléfono", actualizar un APK sobre otro sin desinstalar, notificaciones, modo oscuro del widget.

> Regla para quien continúe: **no marcar como probado lo que no figure acá o en las pruebas automáticas de arriba.** Cuando el autor confirme algo nuevo, agregarlo en esta sección con su versión.

## Lista para que el autor pruebe y avise (marcar al confirmar)

- [ ] Widget: el monto coincide con "Te queda para hoy"; cambia al anotar un gasto y salir de la app.
- [ ] Widget: el botón **+** abre Hoy listo para escribir el monto (con la app cerrada y abierta).
- [ ] Objetivo v9.9.2: escribir un número + Enter cambia "Te queda para hoy"; borrarlo vuelve al límite normal.
- [ ] Ajustes → Exportar: Excel (.xlsx) se abre; PDF se abre.
- [ ] Ajustes → "Guardar en el teléfono": aparece el archivo en Documentos/MiPlata.
- [ ] Recordatorio diario: pide permiso y suena a la hora elegida.
- [ ] Instalar una versión nueva sobre la anterior sin desinstalar (misma clave) y que los datos sigan.

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

- Compilación de la v9.9.2 en GitHub Actions (el autor aún no informó). Las compilaciones de 9.9 y 9.9.1 funcionaron según el autor.
- Ninguna ejecución en un teléfono por parte de la IA. (Desde la v9.9.1 sí hay prueba en navegador de escritorio con `probar_pantallas.py`; el comportamiento real del teclado de Android puede diferir.)
- Instalar el APK nuevo sobre uno viejo (actualización con la clave fija).
- Widget: el autor informó que apareció bien; falta confirmar valores mostrados, botón +, modo oscuro, aviso de dato viejo, actualización al salir de la app y comportamiento con `updatePeriodMillis` (30 min).
- Notificación diaria, permiso de notificaciones en Android 13+.
- Guardar en Documentos/MiPlata en distintas versiones de Android.
- Compartir/guardar el Excel y el PDF desde la app.

## Errores y riesgos conocidos

- **Primera instalación con la clave fija:** un APK firmado con otra clave obliga a desinstalar antes (se pierden los datos si no hay copia). Les va a pasar al autor, a su esposa y a su compañero (que tiene la 9.6).
- **`debug.keystore` en el repositorio:** riesgo de seguridad si el repo es público.
- El recordatorio suena todos los días aunque ya se hayan anotado gastos y puede atrasarse unos minutos.
- El PDF es de texto simple (sin gráficos).
- Se supone, sin haberlo verificado en el código del plugin, que `Directory.DATA` de Capacitor Filesystem en Android equivale a `context.getFilesDir()`, que es donde el widget busca `widget.json`. **(Sin comprobar.)**
- El widget está en Java por la suposición de que el proyecto de Capacitor 6 no compila Kotlin. Esa suposición sigue **sin comprobarse**, pero el Java compiló de verdad en GitHub (el widget se instaló).
- Se supone que `MainActivity.java` generado por Capacitor es una clase vacía en el paquete `com.bustos.miplata`; `agregar_widget.py` la **reemplaza completa** y falla con error si no encuentra `MainActivity.java`.
- Guardar en el teléfono puede fallar en Android 9 o anterior (falta el permiso de almacenamiento, que no se agregó al manifiesto) y se desconoce el comportamiento exacto en Android 11+.

## Qué falta para completar el widget y comprobar que funciona

1. ~~Subir los archivos y correr el workflow~~ (hecho, según el autor).
2. ~~Instalar el APK y agregar el widget~~ (hecho, apareció bien según el autor).
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
