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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var gastoEditandoIndex by remember { mutableStateOf<Int?>(null) }
    var ingresoEditandoIndex by remember { mutableStateOf<Int?>(null) }
    var pendienteDeBorrar by remember { mutableStateOf<DeleteTarget?>(null) }
    val categoriasGasto = listOf("Comida", "Transporte", "Súper", "Salidas", "Salud", "Otro")
    val categoriasIngreso = listOf(
        "Sueldo", "Trabajo independiente", "Efectivo", "Changas o trabajo extra", "Alquileres",
        "Jubilación o pensión", "Ventas o negocio", "Ayuda familiar", "Otros"
    )
    val opcionesCategoriaGasto = if (
        gastoEditandoIndex != null && gastoCategoria.isNotBlank() && gastoCategoria !in categoriasGasto
    ) categoriasGasto + gastoCategoria else categoriasGasto

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
        val editIndex = gastoEditandoIndex
        saveEditedCopy({ edited ->
            val movements = edited.optJSONArray("spent") ?: JSONArray().also { edited.put("spent", it) }
            val movement = if (editIndex != null) {
                movements.optJSONObject(editIndex) ?: error("Ese gasto ya no está en la copia.")
            } else {
                JSONObject()
                    .put("id", System.currentTimeMillis())
                    .put("date", LocalDate.now().toString())
            }
            movement.put("amt", amount)
            movement.put("note", gastoDetalle.trim())
            if (gastoCategoria.isBlank()) movement.remove("cat") else movement.put("cat", gastoCategoria)
            if (editIndex == null) movements.put(movement)
        }, if (editIndex == null) "Gasto guardado en la copia nativa." else "Cambios del gasto guardados.") {
            gastoMonto = ""
            gastoDetalle = ""
            gastoEditandoIndex = null
        }
    }

    fun registerIncome() {
        val amount = parseAmount(ingresoMonto)
        if (amount == null || amount <= 0.0) {
            message = "Ingresá un monto mayor que cero."
            return
        }
        val editIndex = ingresoEditandoIndex
        saveEditedCopy({ edited ->
            val incomes = edited.optJSONArray("ingresos") ?: JSONArray().also { list ->
                val oldSalary = edited.optDouble("sueldo", 0.0)
                if (oldSalary != 0.0) list.put(JSONObject().put("cat", "Sueldo").put("a", oldSalary))
                edited.put("ingresos", list)
            }
            val income = if (editIndex != null) {
                incomes.optJSONObject(editIndex) ?: error("Ese ingreso ya no está en la copia.")
            } else JSONObject()
            income.put("cat", ingresoCategoria).put("a", amount)
            if (editIndex == null) incomes.put(income)
            recomputeSalary(edited, incomes)
        }, if (editIndex == null) "Ingreso agregado a la copia nativa." else "Cambios del ingreso guardados.") {
            ingresoMonto = ""
            ingresoEditandoIndex = null
        }
    }

    fun beginExpenseEdit(entry: ExpenseEntry) {
        gastoEditandoIndex = entry.index
        gastoMonto = entry.amount.toString().replace('.', ',')
        gastoDetalle = entry.note
        gastoCategoria = entry.category
        message = "Editando gasto del ${entry.date.ifBlank { "período actual" }}."
    }

    fun cancelExpenseEdit() {
        gastoEditandoIndex = null
        gastoMonto = ""
        gastoDetalle = ""
        gastoCategoria = "Comida"
        message = null
    }

    fun beginIncomeEdit(entry: IncomeEntry) {
        val index = entry.index ?: return
        ingresoEditandoIndex = index
        ingresoMonto = entry.amount.toString().replace('.', ',')
        ingresoCategoria = entry.category.ifBlank { "Sueldo" }
        message = "Editando ingreso."
    }

    fun cancelIncomeEdit() {
        ingresoEditandoIndex = null
        ingresoMonto = ""
        ingresoCategoria = "Sueldo"
        message = null
    }

    fun confirmDelete(target: DeleteTarget) {
        if (saving) return
        pendienteDeBorrar = target
    }

    fun deleteConfirmed() {
        val target = pendienteDeBorrar ?: return
        if (backup == null) return
        saveEditedCopy({ edited ->
            when (target) {
                is DeleteTarget.Expense -> {
                    val movements = edited.optJSONArray("spent") ?: error("No encontré la lista de gastos.")
                    require(target.index in 0 until movements.length()) { "Ese gasto ya no está en la copia." }
                    movements.remove(target.index)
                }
                is DeleteTarget.Income -> {
                    val incomes = edited.optJSONArray("ingresos") ?: error("No encontré la lista de ingresos.")
                    require(target.index in 0 until incomes.length()) { "Ese ingreso ya no está en la copia." }
                    incomes.remove(target.index)
                    recomputeSalary(edited, incomes)
                }
            }
        }, if (target is DeleteTarget.Expense) "Gasto borrado de la copia nativa." else "Ingreso borrado de la copia nativa.") {
            if (target is DeleteTarget.Expense && gastoEditandoIndex != null) {
                gastoEditandoIndex = null
                gastoMonto = ""
                gastoDetalle = ""
                gastoCategoria = "Comida"
            }
            if (target is DeleteTarget.Income && ingresoEditandoIndex != null) {
                ingresoEditandoIndex = null
                ingresoMonto = ""
                ingresoCategoria = "Sueldo"
            }
            pendienteDeBorrar = null
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
    val incomes = remember(backup) { backup?.let(::readIncomes).orEmpty() }
    val expenses = remember(backup) { backup?.let(::readExpenses).orEmpty() }
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
                            Text(if (gastoEditandoIndex == null) "Anotar gasto" else "Editar gasto", style = MaterialTheme.typography.titleLarge)
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
                            opcionesCategoriaGasto.chunked(2).forEach { rowCategories ->
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
                                Text(if (saving) "Guardando…" else if (gastoEditandoIndex == null) "Guardar gasto" else "Guardar cambios")
                            }
                            if (gastoEditandoIndex != null) {
                                TextButton(onClick = ::cancelExpenseEdit, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancelar edición")
                                }
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (ingresoEditandoIndex == null) "Agregar ingreso" else "Editar ingreso", style = MaterialTheme.typography.titleLarge)
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
                                Text(if (saving) "Guardando…" else if (ingresoEditandoIndex == null) "Guardar ingreso" else "Guardar cambios")
                            }
                            if (ingresoEditandoIndex != null) {
                                TextButton(onClick = ::cancelIncomeEdit, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancelar edición")
                                }
                            }
                            Text("Los cambios se guardan en esta copia local. Exportá una copia para llevarlos a otro teléfono.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Ingresos guardados", style = MaterialTheme.typography.titleLarge)
                            if (incomes.isEmpty()) {
                                Text("Todavía no hay ingresos anotados.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                incomes.asReversed().take(10).forEach { entry ->
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(entry.category.ifBlank { "Ingreso" })
                                            Text(pesos(entry.amount))
                                        }
                                        if (entry.index == null) {
                                            Text("Dato importado; no se modifica desde esta lista.", style = MaterialTheme.typography.bodySmall)
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { beginIncomeEdit(entry) }, enabled = !saving) { Text("Editar") }
                                                TextButton(onClick = { confirmDelete(DeleteTarget.Income(entry.index)) }, enabled = !saving) { Text("Borrar") }
                                            }
                                        }
                                    }
                                }
                                if (incomes.size > 10) Text("Se muestran los últimos 10 ingresos.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Gastos anotados", style = MaterialTheme.typography.titleLarge)
                            if (expenses.isEmpty()) {
                                Text("Todavía no hay gastos anotados.", style = MaterialTheme.typography.bodySmall)
                            } else {
                                expenses.asReversed().take(10).forEach { entry ->
                                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(entry.category.ifBlank { "Gasto" })
                                            Text(pesos(entry.amount))
                                        }
                                        if (entry.note.isNotBlank()) Text(entry.note)
                                        Text(entry.date.ifBlank { "Sin fecha" }, style = MaterialTheme.typography.bodySmall)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = { beginExpenseEdit(entry) }, enabled = !saving) { Text("Editar") }
                                            TextButton(onClick = { confirmDelete(DeleteTarget.Expense(entry.index)) }, enabled = !saving) { Text("Borrar") }
                                        }
                                    }
                                }
                                if (expenses.size > 10) Text("Se muestran los últimos 10 gastos.", style = MaterialTheme.typography.bodySmall)
                            }
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
                Text("Próxima etapa: gestionar los períodos y seguir completando la app nativa.", style = MaterialTheme.typography.bodySmall)
            }
        }
        pendienteDeBorrar?.let { target ->
            val isExpense = target is DeleteTarget.Expense
            AlertDialog(
                onDismissRequest = { if (!saving) pendienteDeBorrar = null },
                title = { Text(if (isExpense) "¿Borrar este gasto?" else "¿Borrar este ingreso?") },
                text = { Text("Se quitará de la copia guardada en esta app. La app original y sus datos no se modifican.") },
                confirmButton = {
                    TextButton(onClick = ::deleteConfirmed, enabled = !saving) { Text(if (saving) "Borrando…" else "Borrar") }
                },
                dismissButton = {
                    TextButton(onClick = { pendienteDeBorrar = null }, enabled = !saving) { Text("Cancelar") }
                }
            )
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

private data class ExpenseEntry(
    val index: Int,
    val date: String,
    val amount: Double,
    val note: String,
    val category: String
)

private data class IncomeEntry(
    val index: Int?,
    val category: String,
    val amount: Double
)

private sealed class DeleteTarget {
    data class Expense(val index: Int) : DeleteTarget()
    data class Income(val index: Int) : DeleteTarget()
}

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

private fun readExpenses(raw: String): List<ExpenseEntry> = runCatching {
    val spent = JSONObject(raw).optJSONArray("spent") ?: return@runCatching emptyList()
    (0 until spent.length()).mapNotNull { index ->
        val movement = spent.optJSONObject(index) ?: return@mapNotNull null
        if (movement.has("meta") && !movement.isNull("meta")) return@mapNotNull null
        val amount = movement.optDouble("amt", 0.0).takeIf { it.isFinite() } ?: return@mapNotNull null
        ExpenseEntry(
            index = index,
            date = movement.optString("date", ""),
            amount = amount,
            note = movement.optString("note", ""),
            category = movement.optString("cat", "")
        )
    }
}.getOrDefault(emptyList())

private fun readIncomes(raw: String): List<IncomeEntry> = runCatching {
    val root = JSONObject(raw)
    val incomes = root.optJSONArray("ingresos")
    if (incomes == null) {
        val salary = root.optDouble("sueldo", 0.0).takeIf { it.isFinite() } ?: 0.0
        return@runCatching if (salary > 0.0) listOf(IncomeEntry(null, "Sueldo importado", salary)) else emptyList()
    }
    (0 until incomes.length()).mapNotNull { index ->
        val income = incomes.optJSONObject(index) ?: return@mapNotNull null
        val amount = income.optDouble("a", 0.0).takeIf { it.isFinite() } ?: return@mapNotNull null
        IncomeEntry(index, income.optString("cat", ""), amount)
    }
}.getOrDefault(emptyList())

private fun recomputeSalary(root: JSONObject, incomes: JSONArray) {
    var total = 0.0
    for (index in 0 until incomes.length()) {
        val amount = incomes.optJSONObject(index)?.optDouble("a", 0.0) ?: 0.0
        if (amount.isFinite()) total += amount
    }
    root.put("sueldo", total)
}
