package com.bustos.miplata.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Read-only port of the daily-budget calculation in index.html's calcular(). */
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

    private data class Discount(val amount: Double, val date: LocalDate?)

    fun calculate(root: JSONObject, today: LocalDate = LocalDate.now()): Result {
        val start = parseDate(root.optString("start"))
            ?: return unsupportedResult(today, "La fecha de inicio del período no es válida.")
        val end = parseDate(root.optString("end"))
            ?: return unsupportedResult(today, "La fecha de fin del período no es válida.")
        val items = root.optJSONArray("items") ?: JSONArray()
        val spent = root.optJSONArray("spent") ?: JSONArray()
        val days = maxOf(1L, ChronoUnit.DAYS.between(start, end))
        val todayIndex = ChronoUnit.DAYS.between(start, today).coerceIn(0, days - 1)
        val modeFund = root.optBoolean("modoFondo")
        val fundRule = if (root.optString("fondoExceso") == "aparte") "aparte" else "siguiente"

        fun listSum(array: JSONArray?, block: (JSONObject) -> Double): Double {
            if (array == null) return 0.0
            var total = 0.0
            for (i in 0 until array.length()) total += block(array.optJSONObject(i) ?: JSONObject())
            return total
        }
        fun amount(item: JSONObject): Double = item.optDouble("a", 0.0).takeIf { it.isFinite() } ?: 0.0
        fun isMeta(entry: JSONObject): Boolean = entry.has("meta") && !entry.isNull("meta")
        fun movementIndex(entry: JSONObject): Long? = parseDate(entry.optString("date"))
            ?.let { ChronoUnit.DAYS.between(start, it) }
        fun spentBetween(first: Long, last: Long): Double = listSum(spent) { entry ->
            val index = movementIndex(entry)
            if (!isMeta(entry) && index != null && index >= first && index <= last)
                entry.optDouble("amt", 0.0) else 0.0
        }

        val totalBase = listSum(items) { item ->
            val increases = listSum(item.optJSONArray("x")) { it.optDouble("amt", 0.0) }
            if (item.optString("d").isNotBlank()) 0.0 else amount(item) - increases
        }
        val contributions = listSum(spent) { if (isMeta(it)) it.optDouble("amt", 0.0) else 0.0 }
        val availableBase = root.optDouble("sueldo", 0.0) + root.optDouble("previo", 0.0) - totalBase - contributions

        val discounts = mutableListOf<Discount>()
        val adjustments = root.optJSONArray("ajustes")
        if (adjustments != null) for (i in 0 until adjustments.length()) {
            val adjustment = adjustments.optJSONObject(i) ?: continue
            discounts += Discount(adjustment.optDouble("amt", 0.0), parseDate(adjustment.optString("desde")))
        }
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val increases = item.optJSONArray("x")
            val increasesTotal = listSum(increases) { it.optDouble("amt", 0.0) }
            if (item.optString("d").isNotBlank()) {
                discounts += Discount(-(amount(item) - increasesTotal), parseDate(item.optString("d")))
            }
            if (increases != null) for (j in 0 until increases.length()) {
                val increase = increases.optJSONObject(j) ?: continue
                discounts += Discount(-increase.optDouble("amt", 0.0), parseDate(increase.optString("desde")))
            }
        }
        var available = availableBase + discounts.sumOf { it.amount }

        val recalculation = root.optJSONObject("recal")
        val recalDay = if (recalculation != null && recalculation.optInt("day", Int.MAX_VALUE) <= todayIndex)
            recalculation.optInt("day").toLong() else null
        val recalBase = recalculation?.optDouble("base")
        fun extra(index: Long): Double = discounts.sumOf { discount ->
            val firstDay = discount.date?.let { maxOf(0L, ChronoUnit.DAYS.between(start, it)) } ?: 0L
            if (index >= firstDay) discount.amount / maxOf(1L, days - firstDay) else 0.0
        }
        fun dayBudget(index: Long): Double = if (recalDay != null && index >= recalDay && recalBase != null)
            recalBase else availableBase / days + extra(index)
        fun spentOn(index: Long): Double = spentBetween(index, index)

        val annualRate = root.optDouble("rendTasa", 0.0).coerceIn(0.0, 300.0)
        val dailyRate = annualRate / 100.0 / 365.0
        val returnDate = parseDate(root.optString("rendDispDesde"))
        val returnStart = returnDate?.let { maxOf(0L, ChronoUnit.DAYS.between(start, it)) } ?: 0L
        val returnActive = root.optBoolean("rendDispOn") && annualRate > 0.0 && returnDate != null
        var pendingReturn = 0.0

        fun addDailyReturn(index: Long, savedBefore: Double, fundWithdrawals: Double) {
            if (!returnActive || index < returnStart) return
            val atStartOfDay = maxOf(0.0, available - spentBetween(Long.MIN_VALUE, index - 1) - savedBefore + fundWithdrawals)
            pendingReturn += atStartOfDay * dailyRate
            val nextDay = start.plusDays(index + 1)
            if (nextDay.dayOfWeek != DayOfWeek.SATURDAY && nextDay.dayOfWeek != DayOfWeek.SUNDAY) {
                discounts += Discount(pendingReturn, nextDay)
                available += pendingReturn
                pendingReturn = 0.0
            }
        }

        var todayBudget: Double
        val todaySpent = spentOn(todayIndex)
        if (!modeFund) {
            for (index in 0 until todayIndex) addDailyReturn(index, 0.0, 0.0)
            val from = recalDay ?: 0L
            var accrued = 0.0
            for (index in from..todayIndex) accrued += dayBudget(index)
            val spentWindow = spentBetween(from, todayIndex)
            todayBudget = accrued - (spentWindow - todaySpent)
        } else {
            val fundStart = parseDate(root.optString("fondoDesde"))
                ?.let { ChronoUnit.DAYS.between(start, it).coerceIn(0, days - 1) } ?: 0L
            val uses = root.optJSONArray("fondoUsos") ?: JSONArray()
            fun useIndex(use: JSONObject): Long = parseDate(use.optString("date"))
                ?.let { ChronoUnit.DAYS.between(start, it).coerceIn(0, days - 1) } ?: 0L
            val extraWithdrawals = DoubleArray(days.toInt())
            for (i in 0 until uses.length()) {
                val use = uses.optJSONObject(i) ?: continue
                if (use.optString("para") != "cubre") extraWithdrawals[useIndex(use).toInt()] += use.optDouble("amt", 0.0)
            }
            fun withdrawalsThrough(index: Long): Double = listSum(uses) { use ->
                if (use.optString("para") != "cubre" && useIndex(use) <= index) use.optDouble("amt", 0.0) else 0.0
            }

            val rows = DoubleArray(days.toInt())
            var carry = 0.0
            var savedBefore = 0.0
            for (index in 0 until days) {
                if (recalDay != null && index.toLong() == recalDay) carry = 0.0
                val withoutCarry = dayBudget(index.toLong()) + extraWithdrawals[index]
                val budget = if (index.toLong() <= todayIndex + 1) withoutCarry + carry else withoutCarry
                val spentOnDay = spentOn(index.toLong())
                val remainder = budget - spentOnDay
                rows[index] = budget
                val beforeFundStarted = index.toLong() < fundStart
                val savedToday = if (beforeFundStarted) 0.0
                    else if (fundRule == "fondo") remainder else maxOf(0.0, remainder)
                carry = if (beforeFundStarted) remainder
                    else if (fundRule == "siguiente") minOf(0.0, remainder) else 0.0
                if (index.toLong() < todayIndex) {
                    addDailyReturn(index.toLong(), savedBefore, withdrawalsThrough(index.toLong()))
                }
                savedBefore += savedToday
            }
            todayBudget = rows[todayIndex.toInt()]
        }

        val todayRemainder = todayBudget - todaySpent
        val allNonMetaSpending = listSum(spent) { if (isMeta(it)) 0.0 else it.optDouble("amt", 0.0) }
        return Result(todayBudget, todaySpent, todayRemainder, available - allNonMetaSpending, today)
    }

    private fun unsupportedResult(today: LocalDate, reason: String) =
        Result(0.0, 0.0, 0.0, 0.0, today, false, reason)

    private fun parseDate(value: String?): LocalDate? = runCatching {
        if (value.isNullOrBlank()) null else LocalDate.parse(value.take(10))
    }.getOrNull()
}
