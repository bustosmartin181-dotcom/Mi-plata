package com.bustos.miplata

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bustos.miplata.data.BackupStore
import com.bustos.miplata.data.BudgetCalculator
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiPlataImportPreview() }
    }
}

@Composable
private fun MiPlataImportPreview() {
    val context = LocalContext.current
    val store = remember { BackupStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val backup by store.backupJson.collectAsState(initial = null)
    var message by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var gastoMonto by remember { mutableStateOf("") }
    var gastoDetalle by remember { mutableStateOf("") }
    var gastoCategoria by remember { mutableStateOf("Comida") }
    var ingresoMonto by remember { mutableStateOf("") }
    var ingresoCategoria by remember { mutableStateOf("Sueldo") }
    var mostrarCategoriasIngreso by remember { mutableStateOf(false) }
    val categoriasGasto = listOf("Comida", "Transporte", "Súper", "Salidas", "Salud", "Otro")
    val categoriasIngreso = listOf(
        "Sueldo", "Trabajo independiente", "Efectivo", "Changas o trabajo extra", "Alquileres",
        "Jubilación o pensión", "Ventas o negocio", "Ayuda familiar", "Otros"
    )

    fun saveEditedCopy(transform: (JSONObject) -> Unit, success: String, afterSave: () -> Unit) {
        if (saving) return
        val current = backup ?: return
        saving = true
        scope.launch {
            try {
                runCatching {
                    val edited = JSONObject(current)
                    transform(edited)
                    store.saveEditedBackup(edited.toString())
                }.onSuccess {
                    message = success
                    afterSave()
                }.onFailure { message = it.message ?: "No pude guardar el cambio." }
            } finally {
                saving = false
            }
        }
    }

    fun registerExpense() {
        val amount = parseAmount(gastoMonto)
        if (amount == null || amount <= 0.0) {
            message = "Ingresá un monto mayor que cero."
            return
        }
        saveEditedCopy({ edited ->
            val movements = edited.optJSONArray("spent") ?: JSONArray().also { edited.put("spent", it) }
            movements.put(
                JSONObject()
                    .put("id", System.currentTimeMillis())
                    .put("date", LocalDate.now().toString())
                    .put("amt", amount)
                    .put("note", gastoDetalle.trim())
                    .put("cat", gastoCategoria)
            )
        }, "Gasto guardado en la copia nativa.") {
            gastoMonto = ""
            gastoDetalle = ""
        }
    }

    fun registerIncome() {
        val amount = parseAmount(ingresoMonto)
        if (amount == null || amount <= 0.0) {
            message = "Ingresá un monto mayor que cero."
            return
        }
        saveEditedCopy({ edited ->
            val incomes = edited.optJSONArray("ingresos") ?: JSONArray().also { list ->
                val oldSalary = edited.optDouble("sueldo", 0.0)
                if (oldSalary != 0.0) list.put(JSONObject().put("cat", "Sueldo").put("a", oldSalary))
                edited.put("ingresos", list)
            }
            incomes.put(JSONObject().put("cat", ingresoCategoria).put("a", amount))
            var total = 0.0
            for (index in 0 until incomes.length()) total += incomes.optJSONObject(index)?.optDouble("a", 0.0) ?: 0.0
            edited.put("sueldo", total)
        }, "Ingreso agregado a la copia nativa.") {
            ingresoMonto = ""
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("No pude abrir ese archivo.")
            }.onSuccess { text ->
                runCatching { store.importOriginalBackup(text) }
                    .onSuccess { message = "Copia importada y guardada en esta app." }
                    .onFailure { message = it.message ?: "No pude importar la copia." }
            }.onFailure { message = it.message ?: "No pude leer el archivo." }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null && backup != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(backup) }
                    ?: error("No pude guardar el archivo.")
            }.onSuccess { message = "Copia exportada." }
                .onFailure { message = it.message ?: "No pude exportar la copia." }
        }
    }

    val summary = remember(backup) { backup?.let(::readSummary) }
    val budget = remember(backup) { backup?.let { runCatching { BudgetCalculator.calculate(JSONObject(it)) }.getOrNull() } }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Mi plata", style = MaterialTheme.typography.headlineMedium)
                Text("Versión nativa · ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("La app que ya usás sigue intacta", style = MaterialTheme.typography.titleMedium)
                        Text("Esta versión es independiente. Podés consultar el presupuesto y anotar ingresos y gastos en la copia nativa; la app original y sus datos siguen separadas.")
                    }
                }
                if (summary == null) {
                    Text("Para empezar, elegí una copia JSON exportada desde Mi Plata.")
                    Button(onClick = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Text("Importar copia de Mi Plata")
                    }
                } else {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Copia importada", style = MaterialTheme.typography.titleLarge)
                            Text("Ingresos: ${summary.incomes}")
                            Text("Gastos importantes: ${summary.importantExpenses}")
                            Text("Movimientos guardados: ${summary.movements}")
                            Text("Período: ${summary.start.ifBlank { "sin fecha" }} al ${summary.end.ifBlank { "sin fecha" }}")
                            Spacer(Modifier.height(2.dp))
                            Text("Se conservó el archivo original completo, incluidos los campos que esta vista previa todavía no muestra.")
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Anotar gasto", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = gastoMonto,
                                onValueChange = { gastoMonto = it },
                                label = { Text("Monto") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = gastoDetalle,
                                onValueChange = { gastoDetalle = it },
                                label = { Text("Detalle (opcional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            categoriasGasto.chunked(2).forEach { rowCategories ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowCategories.forEach { category ->
                                        FilterChip(
                                            selected = gastoCategoria == category,
                                            onClick = { gastoCategoria = category },
                                            label = { Text(category) }
                                        )
                                    }
                                }
                            }
                            Button(onClick = ::registerExpense, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                Text(if (saving) "Guardando…" else "Guardar gasto")
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Agregar ingreso", style = MaterialTheme.typography.titleLarge)
                            OutlinedTextField(
                                value = ingresoMonto,
                                onValueChange = { ingresoMonto = it },
                                label = { Text("Monto") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box {
                                OutlinedButton(onClick = { mostrarCategoriasIngreso = true }) {
                                    Text("Tipo: $ingresoCategoria")
                                }
                                DropdownMenu(
                                    expanded = mostrarCategoriasIngreso,
                                    onDismissRequest = { mostrarCategoriasIngreso = false }
                                ) {
                                    categoriasIngreso.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category) },
                                            onClick = {
                                                ingresoCategoria = category
                                                mostrarCategoriasIngreso = false
                                            }
                                        )
                                    }
                                }
                            }
                            Button(onClick = ::registerIncome, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                Text(if (saving) "Guardando…" else "Guardar ingreso")
                            }
                            Text("Los cambios se guardan en esta copia local. Exportá una copia para llevarlos a otro teléfono.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Presupuesto de hoy · vista previa", style = MaterialTheme.typography.titleLarge)
                            if (budget == null || !budget.supported) {
                                Text(budget?.reason ?: "No pude calcular el presupuesto con esta copia.")
                                Text("No se modificó ningún dato.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                Text("Disponible para hoy: ${pesos(budget.todayBudget)}")
                                Text("Gastado hoy: ${pesos(budget.spentToday)}")
                                Text("Queda hoy: ${pesos(budget.remainingToday)}")
                                Text("Saldo del período: ${pesos(budget.periodBalance)}")
                                Text("Compará estos valores con la pantalla Hoy de la app actual. Esta vista previa es de solo lectura.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                            Text("Importar otra copia")
                        }
                        Button(onClick = { exportPicker.launch("mi-plata-copia-nativa.json") }) {
                            Text("Exportar copia")
                        }
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                Text("Próxima etapa: editar o borrar movimientos y gestionar los períodos.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun pesos(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(value)

private fun parseAmount(raw: String): Double? {
    val value = raw.trim().replace("$", "").replace(" ", "")
    if (value.isEmpty()) return null
    val normalized = when {
        ',' in value -> value.replace(".", "").replace(',', '.')
        value.count { it == '.' } > 1 -> value.replace(".", "")
        '.' in value && value.substringAfterLast('.').length == 3 -> value.replace(".", "")
        else -> value
    }
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}

private data class ImportSummary(
    val incomes: Int,
    val importantExpenses: Int,
    val movements: Int,
    val start: String,
    val end: String
)

private fun readSummary(raw: String): ImportSummary? = runCatching {
    val json = JSONObject(raw)
    ImportSummary(
        incomes = json.optJSONArray("ingresos")?.length() ?: if (json.optDouble("sueldo") > 0) 1 else 0,
        importantExpenses = json.optJSONArray("items")?.length() ?: 0,
        movements = json.optJSONArray("spent")?.length() ?: 0,
        start = json.optString("start", ""),
        end = json.optString("end", "")
    )
}.getOrNull()
