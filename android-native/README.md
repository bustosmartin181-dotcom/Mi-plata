# Mi Plata nativa (vista previa)

Este proyecto empieza una segunda implementación para Android con Kotlin y Jetpack Compose. La app híbrida existente y su flujo de APK quedan separados.

## Estado

- La beta usa el identificador `com.bustos.miplata.preview`, para poder instalarla al lado de la app actual.
- La versión beta actual es `0.1.0-beta.2` (código Android `2`). Se incrementan en cada beta publicable.
- Todas las betas se firman con una clave privada fija guardada en el secreto de GitHub `MI_PLATA_BETA_SIGNING_JSON`. No guardar la clave en el repositorio público.
- La versión de publicación queda reservada con el identificador `com.bustos.miplata`.
- La importación valida y conserva como texto JSON la copia completa de Mi Plata. No elimina ni reescribe campos desconocidos.
- La exportación devuelve ese mismo JSON para facilitar recuperación y compatibilidad.
- La vista previa calcula presupuesto de hoy, gastos, saldo diario y saldo del período para el modo estándar y el modo fondo; no modifica los datos importados.
- Algunas funciones avanzadas todavía no están portadas. Si detecta opciones incompatibles, la vista previa lo informa y no presenta esos valores como equivalentes.
- Esta app todavía no reemplaza las funciones de edición, widgets o recordatorios de la app actual.

## Próximos pasos de la migración

1. Comparar con casos reales el cálculo diario estándar portado a Kotlin; después portar y comparar los modos avanzados.
2. Implementar ingresos, gastos y períodos con guardado local compatible con la copia JSON.
3. Portar metas, cuentas, fondo, cuotas, rendimiento, PIN, exportaciones, recordatorios y widget.
4. Revisar privacidad, accesibilidad, firma de publicación, ficha de Play Store y requisitos vigentes antes de publicar.

La publicación nueva debe generar un Android App Bundle firmado con una clave de carga propia; Play App Signing administra la clave de distribución para las apps nuevas.
