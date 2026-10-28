package com.bustos.miplata.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Read-only port of the standard daily-budget path in index.html's calcular().
 * Advanced modes are rejected until their rules are ported and compared. */
object BudgetCalculator {
    data class Result(
        val todayBudget: Double,
        val spentToday: Double,
        val remainingToday: Double,
        val periodBalance: Double,
        val today: LocalDate,
        val supported: Boolean = true,
        val reason: String? = null
    )

    fun calculate(root: JSONObject, today: LocalDate = LocalDate.now()): Result {
        val unsupported = when {
            root.optBoolean("modoFondo") -> "El modo fondo todavía no está incluido en esta vista previa."
            root.optBoolean("rendDispOn") && root.optDouble("rendTasa") > 0 -> "El rendimiento sobre el disponible todavía no está incluido."
            root.optBoolean("rendOn") && root.optDouble("rendTasa") > 0 -> "El rendimiento del fondo todavía no está incluido."
            else -> null
        }
        if (unsupported != null) return Result(0.0, 0.0, 0.0, 0.0, today, false, unsupported)

        val start = parseDate(root.optString("start"))
            ?: return unsupportedResult(today, "La fecha de inicio del período no es válida.")
        val end = parseDate(root.optString("end"))
            ?: return unsupportedResult(today, "La fecha de fin del período no es válida.")
        val items = root.optJSONArray("items") ?: JSONArray()
        val spent = root.optJSONArray("spent") ?: JSONArray()
        val days = maxOf(1L, ChronoUnit.DAYS.between(start, end))
        val todayIndex = ChronoUnit.DAYS.between(start, today).coerceIn(0, days - 1)

        fun amount(item: JSONObject): Double = item.optDouble("a", 0.0).takeIf { it.isFinite() } ?: 0.0
        fun listSum(array: JSONArray?, block: (JSONObject) -> Double): Double {
            if (array == null) return 0.0
            var total = 0.0
            for (i in 0 until array.length()) total += block(array.optJSONObject(i) ?: JSONObject())
            return total
        }
        val totalBase = listSum(items) { item ->
            if (item.has("d") && item.optString("d").isNotBlank()) 0.0
            else amount(item) - listSum(item.optJSONArray("x")) { it.optDouble("amt", 0.0) }
        }
        val aportes = listSum(spent) { if (it.has("meta") && !it.isNull("meta")) it.optDouble("amt", 0.0) else 0.0 }
        val dispBase = root.optDouble("sueldo", 0.0) + root.optDouble("previo", 0.0) - totalBase - aportes

        data class Discount(val amount: Double, val date: LocalDate?)
        val discounts = mutableListOf<Discount>()
        val adjustments = root.optJSONArray("ajustes")
        if (adjustments != null) for (i in 0 until adjustments.length()) {
            val x = adjustments.optJSONObject(i) ?: continue
            discounts += Discount(x.optDouble("amt", 0.0), parseDate(x.optString("desde")))
        }
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val increases = item.optJSONArray("x")
            val increaseTotal = listSum(increases) { it.optDouble("amt", 0.0) }
            if (item.has("d") && item.optString("d").isNotBlank()) {
                discounts += Discount(-(amount(item) - increaseTotal), parseDate(item.optString("d")))
            }
            if (increases != null) for (j in 0 until increases.length()) {
                val x = increases.optJSONObject(j) ?: continue
                discounts += Discount(-x.optDouble("amt", 0.0), parseDate(x.optString("desde")))
            }
        }

        val recalObject = root.optJSONObject("recal")
        val recalDay = if (recalObject != null && recalObject.optInt("day", Int.MAX_VALUE) <= todayIndex)
            recalObject.optInt("day").toLong() else null
        val recalBase = recalObject?.optDouble("base")
        fun extra(index: Long): Double = discounts.sumOf { discount ->
            val firstDay = discount.date?.let { maxOf(0L, ChronoUnit.DAYS.between(start, it)) } ?: 0L
            if (index >= firstDay) discount.amount / maxOf(1L, days - firstDay) else 0.0
        }
        fun dayBudget(index: Long): Double = if (recalDay != null && index >= recalDay && recalBase != null)
            recalBase else dispBase / days + extra(index)
        fun spentOn(index: Long): Double {
            val date = start.plusDays(index)
            return listSum(spent) { entry ->
                if ((entry.has("meta") && !entry.isNull("meta")) || parseDate(entry.optString("date")) != date) 0.0
                else entry.optDouble("amt", 0.0)
            }
        }

        var accruedBudget = 0.0
        val budgetFrom = recalDay ?: 0L
        for (index in budgetFrom..todayIndex) accruedBudget += dayBudget(index)
        var spentInBudgetWindow = 0.0
        for (index in budgetFrom..todayIndex) spentInBudgetWindow += spentOn(index)
        val todaySpent = spentOn(todayIndex)
        val todayBudget = accruedBudget - (spentInBudgetWindow - todaySpent)
        val remaining = todayBudget - todaySpent
        val spentAll = listSum(spent) {
            if (it.has("meta") && !it.isNull("meta")) 0.0 else it.optDouble("amt", 0.0)
        }
        val available = dispBase + discounts.sumOf { it.amount }
        return Result(todayBudget, todaySpent, remaining, available - spentAll, today)
    }

    private fun unsupportedResult(today: LocalDate, reason: String) =
        Result(0.0, 0.0, 0.0, 0.0, today, false, reason)

    private fun parseDate(value: String?): LocalDate? = runCatching {
        if (value.isNullOrBlank()) null else LocalDate.parse(value.take(10))
    }.getOrNull()
}
