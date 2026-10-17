# Instrucciones para otra IA que continúe Mi plata

Leé este documento completo antes de tocar nada. Está pensado para que puedas seguir sin depender de la conversación original. **Verificá todo contra el código real** antes de proponer cambios: este texto puede quedar desactualizado.

## 1. Qué es

App de Android (híbrida: página web en un WebView, empaquetada con Capacitor 6) que reparte el sueldo de un período en un presupuesto diario y dice cuánto queda para gastar hoy. Es una app **personal/familiar** del autor, Martín Enrique Bustos (usuario de habla hispana, Argentina; hablale en español rioplatense, claro y sin jerga). Todo el código de la app está en un único archivo, `index.html`. No hay servidor.

Detalle: `README.md` (qué hace), `ARQUITECTURA.md` (cómo está hecha).

## 2. Cómo trabaja el autor (importante)

- Trabaja **desde el teléfono**, sin computadora ni Android Studio. Sube archivos a GitHub con "Add file → Upload files" y baja el APK de GitHub Actions.
- Por eso: **entregá archivos completos y listos para subir**, sueltos en la raíz del repositorio. No pidas que suba carpetas ni que corra comandos. Lo único que se edita a mano en GitHub es el workflow (pegando el texto completo).
- No sabe de programación: explicale en simple, y avisale qué archivos tiene que subir.
- Se instala con un APK debug. Cada teléfono tiene sus datos locales (no hay cuentas).

## 3. Estado actual (ver `ESTADO_DEL_PROYECTO.md`)

- Versión del código: **9.9**.
- Hecho y probado en Node: cálculo, objetivo diario, generación de Excel y PDF.
- **Sin compilar ni probar en dispositivo:** v9.8 y v9.9 completas; sobre todo el **widget**, el recordatorio, "Guardar en el teléfono" y la exportación desde Android.
- El **primer paso lógico** es que el autor suba los archivos, corra el workflow y te cuente si compiló. Si falló, pedile las últimas líneas del error del paso que falló.

## 4. Últimos cambios (ver `HISTORIAL_CAMBIOS.md`)

v9.7 objetivo diario → v9.8 guardar en teléfono, Excel/PDF, recordatorio, clave fija → v9.9 números visibles por defecto, widget, ícono nuevo.

## 5. Problemas pendientes

1. Confirmar que compila en GitHub Actions (especialmente `agregar_widget.py` + Java del widget).
2. Probar el widget en un teléfono (lista en `ESTADO_DEL_PROYECTO.md`).
3. Probar recordatorio, "Guardar en el teléfono", Excel y PDF en Android.
4. Primera instalación con la clave fija: los APK viejos (firmados con otra clave) hay que desinstalarlos antes, con copia de seguridad.
5. Riesgo: `debug.keystore` en el repositorio.

No rehagas lo que ya está hecho y probado: núcleo de cálculo, objetivo diario, generadores de Excel/PDF.

## 6. Cómo configurar, ejecutar y probar

- **Pruebas rápidas sin teléfono:** `node probar_nucleo.js` (necesita Node ≥ 18, por `TextEncoder` y `btoa`). Para sintaxis: extraer el contenido de `<script>` y correr `node --check`.
- **Navegador:** abrir `index.html` (no se probó así). Las funciones nativas no hacen nada fuera de la app instalada.
- **APK:** subir los archivos a GitHub → el workflow `construir-apk.yml` corre solo → artefacto `mi-plata-APK`. Tiempo normal: varios minutos.
- **En tu entorno de trabajo** (si tenés uno): no asumas acceso a red ni a Android SDK. Si no los tenés, decí claramente que no podés compilar.

## 7. Reglas para modificar sin romper nada

1. **Leé primero el código real.** No inventes funciones ni nombres. Buscá con `grep` antes de cambiar algo.
2. **Cambios mínimos y localizados.** Es un archivo grande y único: usá reemplazos precisos y verificá que cada texto a reemplazar exista una sola vez.
3. **No toques el núcleo (`calcular()` y vecinos) sin necesidad.** Si lo hacés, corré `probar_nucleo.js` y agregá pruebas.
4. **No cambies `CLAVE_DATOS` ni el formato de `S` sin escribir la migración** en `migrarEstado()`. El autor y su familia tienen datos reales guardados.
5. **No cambies ni pierdas `debug.keystore`.**
6. **Mantené las marcas** `const MS_DIA` y el rótulo `03 · ESTADO` (las usa `probar_nucleo.js`) y el bloque `02b` antes del `03`.
7. **Acción nueva que muestre/exporte datos → agregala a `ACCIONES_PROTEGIDAS`.**
8. **Si cambiás `widget.json` en el JS, cambiá también el Java dentro de `agregar_widget.py`** (y viceversa).
9. Subí la versión (`VERSION_APP`) en cada entrega y registrala en `HISTORIAL_CAMBIOS.md`.
10. Respetá el estilo: comentarios en español, funciones chicas, nombres en español.
11. **No afirmes que algo funciona si no lo probaste.** Distinguí "probado en Node", "compila contra stubs" y "probado en un teléfono".
12. No borres archivos ni reemplaces versiones sin justificarlo. Dejá siempre el estado anterior descrito en el historial.

## 8. Al terminar cada trabajo, informá siempre

- Qué archivos creaste, modificaste o eliminaste.
- Qué pruebas ejecutaste y con qué resultado, y cuáles no pudiste hacer.
- Qué quedó pendiente.
- Qué tiene que subir el autor a GitHub, paso por paso, pensado para hacerlo desde el celular.
- Actualizá `ESTADO_DEL_PROYECTO.md` y `HISTORIAL_CAMBIOS.md`.

## 9. Cómo continuar desde el último estado documentado

1. Pedile al autor el resultado de la última compilación (tilde verde o error) y de la prueba del widget.
2. Si compiló: guiá las pruebas de la lista de `ESTADO_DEL_PROYECTO.md` y corregí lo que falle.
3. Si falló: arreglá según el error real (los pasos "Agregar el widget" y "Construir APK" son los sospechosos) **sin rehacer** lo que ya anda.
4. Recién después, avanzá con el backlog (gráficos por categoría, comparar meses, gastos fijos automáticos, dólares; y como proyecto grande "Mi plata Pro" con usuarios y sincronización, que requiere servidor). Ese backlog **no está implementado**.

## 10. Datos de contacto y privacidad

El repositorio tiene un `AUTOR.txt` con datos de contacto del autor. No los copies a otros lugares ni a documentos públicos. Los datos financieros de la app son del usuario y nunca deben subirse a ningún lado sin su pedido.
