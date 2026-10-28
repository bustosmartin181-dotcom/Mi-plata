# Mi Plata nativa (vista previa)

Este proyecto empieza una segunda implementación para Android con Kotlin y Jetpack Compose. La app híbrida existente y su flujo de APK quedan separados.

## Estado

- La compilación de depuración usa el identificador `com.bustos.miplata.preview`, para poder instalarla al lado de la app actual.
- La versión de publicación queda reservada con el identificador `com.bustos.miplata`.
- La importación valida y conserva como texto JSON la copia completa de Mi Plata. No elimina ni reescribe campos desconocidos.
- La exportación devuelve ese mismo JSON para facilitar recuperación y compatibilidad.
- Esta vista previa es de solo lectura para los datos importados. Todavía no reemplaza funciones, cálculos, widgets o recordatorios de la app actual.

## Próximos pasos de la migración

1. Portar el cálculo diario a Kotlin y compararlo con resultados de referencia de la app actual.
2. Implementar ingresos, gastos y períodos con guardado local compatible con la copia JSON.
3. Portar metas, cuentas, fondo, cuotas, rendimiento, PIN, exportaciones, recordatorios y widget.
4. Revisar privacidad, accesibilidad, firma, ficha de Play Store y requisitos vigentes antes de publicar.

La publicación nueva debe generar un Android App Bundle firmado con una clave de carga propia; Play App Signing administra la clave de distribución para las apps nuevas.
