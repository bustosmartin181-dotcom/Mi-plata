# Historial de cambios

Los cambios de código documentados corresponden a la sesión del **2026-10-16** (una conversación con Claude). Se agregan también las verificaciones y reportes del autor del **2026-10-17**. No existe un historial anterior verificable: el punto de partida fue el APK 9.6 que subió el autor. Qué hizo el autor antes de eso, se desconoce.

Fecha de cambios de código: **2026-10-16**. Actualización de estado y pruebas: **2026-10-17**.

## Punto de partida
- Archivos recibidos: `Mi-plata_v9_6.apk`, `mi-plata-v9_6-codigo.zip` (código extraído del APK: `www/index.html`, configuración, manifiesto), después `Mi-plata-main.zip` (repositorio de GitHub del autor) y `mi-plata-widget-para-github.zip` (un intento previo de widget hecho en otra herramienta).
- Hallazgo: el `index.html` del repositorio coincide en tamaño con el de la 9.6 (98.109 bytes). El `index.html` del zip del widget era **de una versión anterior** a la 9.6 (le faltaban funciones como el botón ＋ flotante y "Ver todos"), por eso **no se usó**; solo se tomó la idea.

## v9.7 — Objetivo diario
- **Motivo:** el autor pidió definir cuánto quiere gastar por día para ver cuánto le sobra para ahorrar.
- **Cambios:** nueva función `objetivoDiario()`, tarjeta "🎯 Mi objetivo por día" en Hoy, botón "Usar el de la app", campo `S.objetivo`.
- **Archivos:** `index.html` (modificado).
- **Pruebas:** cálculo probado en Node con datos de ejemplo; el autor envió captura de la tarjeta funcionando en su teléfono.

## v9.8 — Copias en el teléfono, Excel/PDF, recordatorio, clave fija
- **Motivo:** (a) en el teléfono de la esposa del autor el menú compartir no ofrecía guardar en el almacenamiento; (b) el autor pidió exportar a Excel y PDF y un recordatorio diario; (c) las actualizaciones no se podían instalar encima porque cada compilación se firmaba con una clave distinta.
- **Cambios:** `guardarEnDocumentos()` y botón "📱 Guardar en el teléfono"; generadores `xlsxExportar`/`pdfExportar` (sin librerías); recordatorio con `@capacitor/local-notifications`; `debug.keystore` fija y paso nuevo en el workflow; permisos de notificaciones en el workflow.
- **Archivos:** `index.html` (modificado), `package.json` (modificado: se agregó `@capacitor/local-notifications`), `.github/workflows/construir-apk.yml` (modificado), `debug.keystore` (creado).
- **Pruebas:** Excel abierto con openpyxl y LibreOffice; PDF validado con qpdf y pdftotext; el resto, solo sintaxis. **No se probó en Android.**
- **Corrección del consejo dado antes:** el workflow numera la versión solo (`versionCode` = número de ejecución); no hace falta editar `build.gradle`.

## v9.9 — Números visibles, widget, ícono nuevo
- **Motivo:** el autor vio puntitos en vez de números (era la función "ocultar montos", encendida por defecto y sin ajuste para apagarla); pidió agregar el widget de otro zip, y cambiar el ícono (eligió la opción B).
- **Cambios:**
  - `dineroOculto()` ahora solo oculta si `S.ocultar === true`; casilla en Ajustes → Apariencia; el botón 👁 solo aparece si está activado.
  - Widget: `actualizarWidgetResumen()` escribe `widget.json`; `window.miplataAbrir`; `agregar_widget.py` instala el widget (Java, no Kotlin, porque el proyecto base de Capacitor normalmente no compila Kotlin **(sin comprobar)**) y reemplaza `MainActivity.java`; paso nuevo en el workflow.
  - Íconos nuevos (`icon-only`, `icon-foreground`, `icon-background`, `splash`, `splash-dark`), generados con Python/Pillow.
  - Documentación y `probar_nucleo.js`.
- **Archivos creados:** `agregar_widget.py`, `README.md`, `ESTADO_DEL_PROYECTO.md`, `ARQUITECTURA.md`, `HISTORIAL_CAMBIOS.md`, `INSTRUCCIONES_PARA_OTRA_IA.md`, `probar_nucleo.js`, los cinco PNG (reemplazan a los anteriores del repositorio).
- **Archivos modificados:** `index.html`, `construir-apk.yml`.
- **Archivos eliminados:** ninguno. `index-2.html` del repositorio quedó sin usar y sin tocar.
- **Pruebas:** ver `ESTADO_DEL_PROYECTO.md` (simulación de `agregar_widget.py`, compilación del Java contra clases de reemplazo, prueba del contenido de `widget.json`). **El widget no se compiló ni se instaló.**

## v9.9.1 — Objetivo en Días, Anotar gasto siempre debajo de "Te queda para hoy", botón azul a la vista
- **Motivo:** el autor pidió (a) mover "Mi objetivo por día" a la solapa Días; (b) que "Anotar gasto" esté siempre debajo de "Te queda para hoy"; (c) que al tocar el botón ＋ el campo Monto y el botón azul "Anotar gasto" se vean juntos: el teclado tapaba el botón y había que subir la pantalla.
- **Cambios:** `pantallaHoy` ya no incluye `tarjetaObjetivo`; `pantallaDias` la muestra primero; la tarjeta de anotar tiene `id="tarjeta-anotar"`; con un campo enfocado se agrega la clase `teclado` al `body` (esconde la barra, el ＋ y el degradado de abajo) y, si el campo está en esa tarjeta, se la lleva a la vista con `scrollIntoView({block:'end'})` (dos intentos, a los 350 y 800 ms); `VERSION_APP` pasa a `9.9.1`.
- **Archivos:** `index.html` y `www/index.html` (modificados), `probar_pantallas.py` (creado), documentos (actualizados).
- **Pruebas:** sintaxis; `probar_nucleo.js`; `probar_pantallas.py` en Chromium sin ventana. **Sin probar con el teclado real de Android.**
- **Incidente a tener en cuenta:** al empezar este cambio, el `index.html` de la raíz de la carpeta de trabajo era distinto del que se entregó como v9.9: tenía cambios que no se hicieron en esta conversación y le faltaban las funciones `tarjetaGastosAnotados` y `tarjetaRecalibrar` (la pantalla Hoy habría fallado). No se usó. Se partió de la v9.9 entregada (idéntica a `www/index.html`), y la raíz quedó reemplazada por la v9.9.1. Se desconoce quién la modificó.

## v9.9.2 — El objetivo por día cambia el límite oficial
- **Motivo:** el autor notó que "Mi objetivo por día" era solo un simulador y pidió: escribir un número, apretar Enter, y que cambie la cifra oficial.
- **Cambios:**
  - Nueva `aplicarObjetivo(valor)`: guarda `S.objetivo` y `S.objetivoDesde` y crea `S.recal = {day: hoy, base: objetivo, obj: true}`. Se reutiliza la recalibración existente: desde hoy cada día vale el objetivo y lo no gastado se arrastra.
  - Borrar el número saca la recalibración solo si tiene la marca `obj: true` (no toca una hecha a mano). "Recalibrar" y "Quitar" recalibración ahora ponen el objetivo en 0 (no pueden convivir). Repetir el mismo número no reinicia nada.
  - Enter en el campo confirma y cierra el teclado. Aviso "Listo: tu límite de hoy ahora es $X".
  - Un objetivo guardado por versiones anteriores **no se aplica solo**: la tarjeta avisa "todavía no es tu límite oficial" y ofrece el botón "Usar como mi límite de hoy" (`usarObjetivo`).
  - "Ahorrado vs. objetivo" ahora cuenta solo desde `objetivoDesde` (antes comparaba contra días anteriores al objetivo y daba negativos engañosos).
  - "Usar el de la app" calcula el límite sin el objetivo.
  - Se corrigió un fallo encontrado en la prueba: al confirmar con Enter la pantalla se redibuja con el campo enfocado y la barra de abajo quedaba escondida; ahora un `MutationObserver` la restituye.
  - `VERSION_APP` = `9.9.2`.
- **Archivos:** `index.html` y `www/index.html` (modificados), `probar_pantallas.py` (reescrito, con fechas relativas y comprobaciones), `probar_nucleo.js` (ampliado), documentos.
- **Pruebas:** sintaxis; `probar_nucleo.js`; `probar_pantallas.py` (13 comprobaciones, todas bien) en Chromium sin ventana. **Sin probar en el teléfono ni con el teclado real de Android.**
- **Nota:** el widget y las exportaciones leen `calcular()`, por lo que reflejan el nuevo límite, pero no se volvió a probar el widget (nunca se probó en dispositivo).

## Confirmaciones del autor durante la sesión (reportes suyos)
- Tras instalar la v9.9: el widget apareció en el selector y se veía "perfecto".
- Tras instalar la v9.9.1: "ahora sí anda todo"; le encantó "Mi objetivo por día".
- Pidió dejar los documentos "como si todas las funciones estuvieran probadas". No se hizo así: quedó registrado solo lo que él confirmó, y el resto figura como sin probar con una lista de pruebas en `ESTADO_DEL_PROYECTO.md`.

## Verificación y reportes posteriores — 2026-10-17 (sin cambios en la app)

- **GitHub Actions:** la ejecución [#11 — Construir APK](https://github.com/bustosmartin181-dotcom/Mi-plata/actions/runs/39041786458) terminó con `success`; los pasos de construir el APK y subir `mi-plata-APK` pasaron. El commit compilado fue `962f1a00a4beb4a53dbfccfcae215122046250c5`.
- **Correspondencia con el ZIP:** los archivos `index.html`, `.github/workflows/construir-apk.yml` y `agregar_widget.py` del commit de la ejecución coinciden con los del código recibido.
- **Pruebas en la sesión:** `node probar_nucleo.js`, `node --check` del JavaScript de `index.html` y `probar_pantallas.py` en Chromium (13 comprobaciones) pasaron.
- **Reportes del autor desde el teléfono:** el widget se instaló y se probó; informa que funciona perfecto. La modificación del objetivo de v9.9.2 también funciona perfectamente. Además, exportó una copia a Drive y la restauró desde la nube al reinstalar.
- **Sin reporte de prueba aún:** exportaciones Excel/PDF en Android, guardado local en Documentos/MiPlata, recordatorio diario y actualización de un APK sobre otro sin desinstalar.
- **Archivos documentales actualizados:** `ESTADO_DEL_PROYECTO.md` y este `HISTORIAL_CAMBIOS.md`. No se modificó el código de la app; `VERSION_APP` permanece en **9.9.2**.

## Entregas
- El zip `mi-plata-v9_9-codigo.zip` contiene todo el estado final. Los zips de 9.7 y 9.8 que se entregaron antes en la conversación fueron reemplazados por este; no hay APK generado por la IA.
