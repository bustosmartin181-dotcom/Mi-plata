# Estado del proyecto — actualización al 2026-10-17

Versión del código: **9.9.2** (`VERSION_APP` en `index.html`). Base: código extraído del APK **9.6** que subió el autor.

## Resumen honesto

| Parte | Estado |
|---|---|
| Cálculo del presupuesto diario (núcleo) | Funciona en pruebas de Node. Es código que el autor ya usaba en el APK 9.6. |
| Objetivo diario (v9.7) | Probado en Node. **Además el autor mandó una captura** de la tarjeta funcionando en su teléfono (una versión 9.7/9.8; no se verificó cuál). |
| Exportar Excel y PDF (v9.8) | Archivos de prueba generados y abiertos con openpyxl y LibreOffice (xlsx), qpdf y pdftotext (pdf). **No se probó en Android** (guardado + menú compartir). |
| Guardar en el teléfono (v9.8) | **Sin probar en ningún teléfono.** Depende de que Android permita escribir en Documentos. |
| Recordatorio diario (v9.8) | **Sin probar.** Plugin y permisos agregados por el workflow; nunca se vio sonar una notificación. |
| Copia en Drive/nube y restauración (v9.8) | El autor informó que exportó la copia a Drive y la restauró desde la nube al reinstalar. |
| Actualizar un APK ya instalado | El autor informa que Android no le permite actualizar a una versión más nueva. Versión instalada y mensaje exacto desconocidos; causa sin confirmar. |
| Números visibles por defecto (v9.9) | Cambio de 3 líneas; sintaxis comprobada; **sin probar en pantalla**. |
| Objetivo como límite oficial (v9.9.2) | Probado en navegador real y Node; el autor informó que la modificación funciona perfectamente en su teléfono. |
| Reordenado de Hoy/Días y teclado (v9.9.1) | **Probado en un navegador real** (Chromium sin ventana, teclado simulado). **Además el autor, tras instalar esta versión, escribió "ahora sí anda todo"** (reporte suyo, general; no detalló punto por punto). |
| Widget "Mi plata — Hoy" (v9.9) | El autor informó que lo instaló, lo probó en su teléfono y funciona perfecto. No detalló cada caso de prueba por separado. |
| Ícono nuevo (v9.9) | Imágenes generadas y revisadas visualmente; **no se vio en un teléfono**. |

## Confirmado por el autor en su teléfono (reportes del autor; la IA no pudo verificarlos)

- **v9.9:** la compilación en GitHub Actions con el widget funcionó; el widget apareció en el selector y se veía "perfecto".
- **v9.9.1:** "ahora sí anda todo": objetivo en Días, "Anotar gasto" debajo de "Te queda para hoy" y botón azul visible con el teclado. El autor dijo que le encantó "Mi objetivo por día".
- **v9.9.2:** el autor informó que el cambio del objetivo funciona perfectamente en su teléfono.
- **Widget:** el autor informó que lo instaló, lo probó y funciona perfecto.
- **Copia en Drive/nube:** el autor informó que exportó a Drive y restauró desde la nube al reinstalar.
- **Actualización de la app:** el autor informa que una versión nueva no se instala sobre la anterior; no identifica la versión anterior ni un mensaje de Android.
- **Sin reporte de prueba en el teléfono:** recordatorio diario, exportaciones Excel/PDF, "Guardar en el teléfono" (Documentos/MiPlata), actualizar un APK sobre otro sin desinstalar, notificaciones y casos específicos de modo oscuro/ocultar montos/dato viejo del widget.

> Regla para quien continúe: **no marcar como probado lo que no figure acá o en las pruebas automáticas de arriba.** Cuando el autor confirme algo nuevo, agregarlo en esta sección con su versión.

## Verificación realizada el 2026-10-17

1. GitHub Actions, ejecución [#11](https://github.com/bustosmartin181-dotcom/Mi-plata/actions/runs/39041786458), terminó con `success`. Pasaron el paso **Construir APK** y la carga del artefacto `mi-plata-APK`. El commit compilado fue `962f1a00a4beb4a53dbfccfcae215122046250c5`; su `index.html`, workflow y `agregar_widget.py` coinciden con los del ZIP recibido.
2. `node probar_nucleo.js`: pasaron las pruebas del núcleo, el objetivo y la generación XLSX/PDF.
3. `node --check` sobre el JavaScript de `index.html`: sin errores de sintaxis.
4. `probar_pantallas.py` en Chromium: 13 comprobaciones pasaron (pantallas Hoy/Días, teclado simulado, anotación de gasto y objetivo).
5. La IA no probó la app en un dispositivo Android; las pruebas del widget, el objetivo y la copia/restauración en teléfono son reportes del autor.

## Lista para que el autor pruebe y avise (marcar al confirmar)

- [x] Widget: el autor informó que lo instaló y probó; dice que funciona perfecto (sin desglose de casos).
- [x] Objetivo v9.9.2: el autor confirma que la modificación funciona perfectamente en el teléfono.
- [x] Copia en Drive/nube: el autor exportó y restauró desde la nube al reinstalar.
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

- Compilación local de la v9.9.2 por la IA (este entorno no tiene Android SDK); GitHub Actions sí la compiló con éxito en la ejecución #11.
- Ninguna ejecución en un teléfono por parte de la IA. El autor sí reportó pruebas del widget, el objetivo y la copia/restauración desde la nube.
- No se reprodujo el rechazo al actualizar. Las ejecuciones #10 y #11 usan el mismo `appId` y el mismo `debug.keystore`; el workflow asigna `versionCode` según el número de ejecución (10 y 11). Si el APK instalado es anterior a la clave fija, una firma distinta sigue siendo una causa posible. Falta identificar versión anterior y mensaje de Android.
- Widget: el autor informó que funciona perfecto. No detalló por separado modo oscuro, ocultar montos, aviso de dato viejo ni cada condición de actualización.
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

## Estado actual del widget

El autor confirma que el widget se instaló, se probó y funciona perfecto. No informó por separado los casos de modo oscuro, montos ocultos y dato de otro día.

## Problemas que no se resolvieron y por qué

- La IA no pudo compilar localmente: este entorno no tiene Android SDK. Sí se verificó la compilación del APK en GitHub Actions, ejecución #11.
- La IA no tiene un dispositivo Android para probar el widget ni las notificaciones; el autor reportó que el widget funciona perfecto. El recordatorio sigue sin reporte de prueba.

## Tareas pendientes (del backlog, **no implementadas**)

Ideas conversadas con el autor, sin código: gráficos por categoría, comparar meses, gastos fijos automáticos, cuentas en dólares, presupuesto por categoría ("10.0"); inicio de sesión con usuarios, sincronización entre teléfonos y presupuesto familiar compartido ("Mi plata Pro", requiere servidor); firma `release` y Play Store; botón para pasar lo ahorrado a una meta.

## Archivos creados, modificados y eliminados en esta sesión

Ver `HISTORIAL_CAMBIOS.md`.
