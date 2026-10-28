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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private data class MiPlataTheme(
    val id: String, val name: String, val dark: Color, val primary: Color, val accent: Color,
    val background: Color, val surface: Color, val text: Color, val muted: Color,
    val border: Color, val soft: Color, val onSoft: Color
) {
    val colors: ColorScheme = lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = soft, onPrimaryContainer = onSoft,
        secondary = accent, secondaryContainer = soft, onSecondaryContainer = onSoft,
        background = background, onBackground = text,
        surface = surface, onSurface = text, surfaceVariant = soft,
        onSurfaceVariant = muted, outline = border, error = Color(0xFFB83E48)
    )
}

private val MiPlataThemes = listOf(
    MiPlataTheme("one_ui", "One UI", Color(0xFF174B39), Color(0xFF176B4A), Color(0xFF26835D), Color(0xFFF4F2EC), Color(0xFFFFFEFA), Color(0xFF19251F), Color(0xFF536359), Color(0xFFE1E3DA), Color(0xFFE2F0E7), Color(0xFF174B39)),
    MiPlataTheme("lavanda", "Lavanda", Color(0xFF4C2A78), Color(0xFF6D4BA1), Color(0xFF9271BD), Color(0xFFF5F1F8), Color(0xFFFFFCFF), Color(0xFF28222E), Color(0xFF675E70), Color(0xFFE5DCEA), Color(0xFFEDE3F5), Color(0xFF432665)),
    MiPlataTheme("oceano", "Océano", Color(0xFF16436D), Color(0xFF1964A5), Color(0xFF3284BE), Color(0xFFF1F5F8), Color(0xFFFFFFFF), Color(0xFF1B2530), Color(0xFF5B6977), Color(0xFFDFE7EC), Color(0xFFE4F0F7), Color(0xFF183F61))
)

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
    val preferences = remember { context.getSharedPreferences("preferencias_visuales", android.content.Context.MODE_PRIVATE) }
    var themeId by remember { mutableStateOf(preferences.getString("tema", "one_ui") ?: "one_ui") }
    val selectedTheme = MiPlataThemes.firstOrNull { it.id == themeId } ?: MiPlataThemes.first()
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
    var mostrarNuevoPeriodo by remember { mutableStateOf(false) }
    var periodoNuevoPrevio by remember { mutableStateOf("") }
    var periodoNuevoInicio by remember { mutableStateOf("") }
    var periodoNuevoFin by remember { mutableStateOf("") }
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

    fun abrirNuevoPeriodo() {
        val current = backup?.let(::JSONObject) ?: return
        val end = runCatching { LocalDate.parse(current.optString("end")) }.getOrNull() ?: LocalDate.now()
        val calc = budget ?: return
        val defaultPrevious = maxOf(0.0, calc.periodBalance - if (calc.fundActive) calc.fundSavedPeriod else 0.0)
        periodoNuevoPrevio = (kotlin.math.round(defaultPrevious * 100.0) / 100.0).toString()
        periodoNuevoInicio = end.toString()
        periodoNuevoFin = end.plusMonths(1).toString()
        mostrarNuevoPeriodo = true
    }

    fun confirmarNuevoPeriodo() {
        if (backup == null) return
        val previous = parseAmount(periodoNuevoPrevio)
        val startDate = runCatching { LocalDate.parse(periodoNuevoInicio.trim()) }.getOrNull()
        val endDate = runCatching { LocalDate.parse(periodoNuevoFin.trim()) }.getOrNull()
        if (previous == null || previous < 0.0) {
            message = "Revisá el saldo anterior: tiene que ser cero o mayor."
            return
        }
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            message = "Revisá las fechas: el fin debe ser posterior al inicio."
            return
        }
        val calc = budget
        if (calc == null || !calc.supported) {
            message = "No pude calcular este período; no se modificó ningún dato."
            return
        }
        saveEditedCopy({ edited ->
            finishNativePeriod(edited, calc, previous, startDate, endDate)
        }, "Período cerrado y nuevo período iniciado. Las cuotas y los gastos repetidos se conservaron.") {
            mostrarNuevoPeriodo = false
        }
    }
    MaterialTheme(colorScheme = selectedTheme.colors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(Brush.linearGradient(listOf(selectedTheme.dark, selectedTheme.primary, selectedTheme.accent)))
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mi plata", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                        Text("El pulso de tus gastos, en un solo lugar.", color = Color.White.copy(alpha = 0.88f))
                        Text("Versión nativa · ${BuildConfig.VERSION_NAME}", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("La app que ya usás sigue intacta", style = MaterialTheme.typography.titleMedium)
                        Text("Esta versión es independiente. Podés consultar el presupuesto y anotar ingresos y gastos en la copia nativa; la app original y sus datos siguen separadas.")
                    }
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Colores de la app", style = MaterialTheme.typography.titleMedium)
                        Text("Elegí el tema que más te guste. Se guarda en este teléfono.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiPlataThemes.forEach { theme ->
                                FilterChip(
                                    selected = theme.id == selectedTheme.id,
                                    onClick = {
                                        themeId = theme.id
                                        preferences.edit().putString("tema", theme.id).apply()
                                    },
                                    label = { Text(theme.name) }
                                )
                            }
                        }
                    }
                }
                if (summary == null) {
                    Text("Para empezar, elegí una copia JSON exportada desde Mi Plata.")
                    Button(onClick = { importPicker.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Text("Importar copia de Mi Plata")
                    }
                } else {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = selectedTheme.soft)) {
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
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Período", style = MaterialTheme.typography.titleLarge)
                            Text("Al avanzar, se guarda el resumen actual. Las cuotas pendientes, los gastos repetidos y el fondo siguen en la copia nativa.")
                            Button(onClick = ::abrirNuevoPeriodo, enabled = !saving && budget?.supported == true, modifier = Modifier.fillMaxWidth()) {
                                Text("Cerrar período e iniciar otro")
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
                Text("La app original y sus datos siguen separados de esta copia nativa.", style = MaterialTheme.typography.bodySmall)
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
        if (mostrarNuevoPeriodo) {
            AlertDialog(
                onDismissRequest = { if (!saving) mostrarNuevoPeriodo = false },
                title = { Text("Iniciar un período nuevo") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Se guardará el resumen del período actual. Revisá el saldo que querés pasar al nuevo período.")
                        OutlinedTextField(
                            value = periodoNuevoPrevio,
                            onValueChange = { periodoNuevoPrevio = it },
                            label = { Text("Saldo anterior") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                        OutlinedTextField(
                            value = periodoNuevoInicio,
                            onValueChange = { periodoNuevoInicio = it },
                            label = { Text("Inicio (AAAA-MM-DD)") }, singleLine = true
                        )
                        OutlinedTextField(
                            value = periodoNuevoFin,
                            onValueChange = { periodoNuevoFin = it },
                            label = { Text("Fin (AAAA-MM-DD)") }, singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = ::confirmarNuevoPeriodo, enabled = !saving) {
                        Text(if (saving) "Guardando…" else "Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarNuevoPeriodo = false }, enabled = !saving) { Text("Cancelar") }
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

/** Applies the original app's period rollover rules to a copy of the imported JSON. */
private fun finishNativePeriod(
    root: JSONObject,
    result: BudgetCalculator.Result,
    previous: Double,
    start: LocalDate,
    end: LocalDate
) {
    val oldStart = root.optString("start")
    val oldEnd = root.optString("end")
    val history = root.optJSONArray("hist") ?: JSONArray().also { root.put("hist", it) }
    val historyRow = JSONObject()
        .put("start", oldStart)
        .put("end", oldEnd)
        .put("sueldo", root.optDouble("sueldo", 0.0))
        .put("previo", root.optDouble("previo", 0.0))
        .put("importantes", result.importantTotal)
        .put("gastado", result.spentTotal)
        .put("quedo", result.periodBalance)
    if (result.fundActive) historyRow.put("ahorro", result.fundSavedPeriod)
    history.put(historyRow)

    if (result.fundActive) {
        root.put("fondoPrev", result.fundTotal)
        root.put("fondoRetirado", root.optDouble("fondoRetirado", 0.0) + result.fundUses)
        root.put("fondoUsos", JSONArray())
        if (root.optBoolean("rendOn")) {
            root.put("rendPrev", result.fundReturn + result.fundPendingReturn)
            root.put("rendDesde", start.toString())
        }
    }

    val items = root.optJSONArray("items") ?: JSONArray()
    val retained = JSONArray()
    for (i in 0 until items.length()) {
        val item = items.optJSONObject(i) ?: continue
        val installment = item.optJSONObject("q")
        val hasInstallments = installment != null && installment.optInt("total", 0) > 1
        if (hasInstallments && installment!!.optInt("actual", 0) >= installment.optInt("total", 0)) continue
        if (!hasInstallments && item.has("f") && !item.optBoolean("f")) continue
        if (hasInstallments) installment!!.put("actual", installment.optInt("actual", 0) + 1)
        item.put("p", false)
        item.remove("d")
        item.remove("x")
        val nextAmount = item.opt("prox")
        if (nextAmount != null && nextAmount != JSONObject.NULL && nextAmount.toString().isNotBlank()) {
            item.put("a", nextAmount.toString().toDoubleOrNull() ?: 0.0)
        } else if (item.optBoolean("v") && !hasInstallments) {
            item.put("a", 0)
        }
        item.remove("prox")
        retained.put(item)
    }
    val upcoming = root.optJSONArray("proximos") ?: JSONArray()
    for (i in 0 until upcoming.length()) {
        val item = upcoming.optJSONObject(i) ?: continue
        val name = item.optString("n")
        val amount = item.optDouble("a", 0.0)
        if (name.isNotBlank() || amount != 0.0) {
            retained.put(JSONObject().put("n", name).put("a", amount).put("p", false).put("f", false).put("v", false))
        }
    }
    root.put("items", retained)
    root.put("proximos", JSONArray())
    root.put("fondoDesde", "")
    if (root.optBoolean("rendDispOn")) root.put("rendDispDesde", start.toString())
    root.put("cierres", JSONObject())
    root.put("ajustes", JSONArray())
    root.put("spent", JSONArray())
    root.remove("recal")
    root.put("previo", previous)
    root.put("start", start.toString())
    root.put("end", end.toString())
}
