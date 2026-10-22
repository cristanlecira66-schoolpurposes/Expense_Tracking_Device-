
package com.example.expensetrackingsystem

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var txtReportMonth: TextView
    private lateinit var txtReportIncome: TextView
    private lateinit var txtReportExpenses: TextView
    private lateinit var txtReportBalance: TextView
    private lateinit var txtCategoryEmpty: TextView
    private lateinit var txtMonthlyEmpty: TextView

    private lateinit var categoryReportChart: PieChart
    private lateinit var monthlyReportChart: BarChart

    private val dateFormat =
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)

    private data class TransactionRecord(
        val amount: Double,
        val type: String,
        val category: String,
        val date: Date?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        txtReportMonth = findViewById(R.id.txtReportMonth)
        txtReportIncome = findViewById(R.id.txtReportIncome)
        txtReportExpenses = findViewById(R.id.txtReportExpenses)
        txtReportBalance = findViewById(R.id.txtReportBalance)
        txtCategoryEmpty = findViewById(R.id.txtCategoryEmpty)
        txtMonthlyEmpty = findViewById(R.id.txtMonthlyEmpty)

        categoryReportChart =
            findViewById(R.id.categoryReportChart)

        monthlyReportChart =
            findViewById(R.id.monthlyReportChart)

        findViewById<Button>(R.id.btnBackAnalytics)
            .setOnClickListener {
                finish()
            }

        setupCharts()
    }

    override fun onResume() {
        super.onResume()
        refreshReports()
    }

    private fun setupCharts() {
        categoryReportChart.apply {
            setUsePercentValues(false)

            description = Description().apply {
                text = ""
            }

            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 63f
            setHoleColor(Color.WHITE)
            setEntryLabelColor(Color.DKGRAY)
            setEntryLabelTextSize(11f)

            legend.isEnabled = true
            legend.textColor = Color.DKGRAY
            legend.textSize = 12f

            legend.verticalAlignment =
                com.github.mikephil.charting.components.Legend
                    .LegendVerticalAlignment.BOTTOM

            legend.horizontalAlignment =
                com.github.mikephil.charting.components.Legend
                    .LegendHorizontalAlignment.CENTER

            legend.orientation =
                com.github.mikephil.charting.components.Legend
                    .LegendOrientation.HORIZONTAL

            legend.setDrawInside(false)

            setNoDataText("No expense data available")
            setNoDataTextColor(Color.GRAY)
        }

        monthlyReportChart.apply {
            description = Description().apply {
                text = ""
            }

            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(true)

            legend.isEnabled = false
            axisRight.isEnabled = false

            axisLeft.axisMinimum = 0f
            axisLeft.textColor = Color.GRAY
            axisLeft.textSize = 11f
            axisLeft.gridColor = Color.rgb(232, 236, 233)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            xAxis.textColor = Color.GRAY
            xAxis.textSize = 10f
            xAxis.labelRotationAngle = 0f

            setNoDataText("No monthly expense data available")
            setNoDataTextColor(Color.GRAY)
        }
    }

    private fun refreshReports() {
        val transactions = loadTransactions()

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        txtReportMonth.text = SimpleDateFormat(
            "MMMM yyyy",
            Locale.getDefault()
        ).format(calendar.time) + " Overview"

        val currentMonthTransactions = transactions.filter { record ->
            record.date?.let { date ->
                val transactionCalendar = Calendar.getInstance()
                transactionCalendar.time = date

                transactionCalendar.get(Calendar.MONTH) == currentMonth &&
                        transactionCalendar.get(Calendar.YEAR) == currentYear
            } ?: false
        }

        val income = currentMonthTransactions
            .filter {
                it.type.equals("Income", ignoreCase = true)
            }
            .sumOf { it.amount }

        val expenses = currentMonthTransactions
            .filter {
                it.type.equals("Expense", ignoreCase = true)
            }
            .sumOf { it.amount }

        val balance = income - expenses

        txtReportIncome.text = formatMoney(income)
        txtReportExpenses.text = formatMoney(expenses)
        txtReportBalance.text = formatMoney(balance)

        showCategoryReport(currentMonthTransactions)
        showMonthlyReport(transactions)
    }

    private fun loadTransactions(): List<TransactionRecord> {

        // Read the currently logged-in account.
        val currentEmail = getSharedPreferences(
            "ExpenseTrackerSession",
            MODE_PRIVATE
        )
            .getString("currentUserEmail", "")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

        if (currentEmail.isBlank()) {
            return emptyList()
        }

        // Match the storage used by AddExpenseActivity.
        val preferences = getSharedPreferences(
            "ExpenseTrackerData_$currentEmail",
            MODE_PRIVATE
        )

        val jsonString = preferences.getString(
            "transactions_data",
            "[]"
        ) ?: "[]"

        val records = mutableListOf<TransactionRecord>()

        try {
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optJSONObject(i) ?: continue

                val amount = item.optDouble("amount", 0.0)
                val type = item.optString("type", "")
                val category = item.optString("category", "Others")
                val dateString = item.optString("date", "")

                val parsedDate = try {
                    if (dateString.isNotBlank()) {
                        dateFormat.parse(dateString)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }

                records.add(
                    TransactionRecord(
                        amount = amount,
                        type = type,
                        category = category,
                        date = parsedDate
                    )
                )
            }
        } catch (_: Exception) {
            // Keep the report screen open if saved data is invalid.
        }

        return records
    }

    private fun showCategoryReport(
        transactions: List<TransactionRecord>
    ) {
        val categoryTotals = transactions
            .filter {
                it.type.equals("Expense", ignoreCase = true) &&
                        it.amount > 0
            }
            .groupBy {
                it.category.ifBlank { "Others" }
            }
            .mapValues { entry ->
                entry.value.sumOf { it.amount }
            }
            .filterValues { it > 0 }
            .toSortedMap()

        if (categoryTotals.isEmpty()) {
            categoryReportChart.clear()
            categoryReportChart.visibility = View.GONE
            txtCategoryEmpty.visibility = View.VISIBLE
            return
        }

        txtCategoryEmpty.visibility = View.GONE
        categoryReportChart.visibility = View.VISIBLE

        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount.toFloat(), category)
        }

        val colors = listOf(
            Color.rgb(46, 139, 99),
            Color.rgb(72, 135, 196),
            Color.rgb(240, 166, 66),
            Color.rgb(218, 91, 86),
            Color.rgb(137, 105, 190),
            Color.rgb(63, 171, 174),
            Color.rgb(224, 126, 67),
            Color.rgb(130, 145, 140)
        )

        val dataSet = PieDataSet(
            entries,
            "Expense Categories"
        ).apply {
            this.colors = colors
            sliceSpace = 3f
            selectionShift = 6f
            valueTextSize = 11f
            valueTextColor = Color.WHITE

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "₱" + String.format(
                        Locale.getDefault(),
                        "%,.0f",
                        value
                    )
                }
            }
        }

        categoryReportChart.data = PieData(dataSet)
        categoryReportChart.centerText = "This Month"
        categoryReportChart.setCenterTextSize(14f)
        categoryReportChart.setCenterTextColor(
            Color.rgb(23, 61, 50)
        )

        categoryReportChart.animateY(700)
        categoryReportChart.invalidate()
    }

    private fun showMonthlyReport(
        transactions: List<TransactionRecord>
    ) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val monthLabels = mutableListOf<String>()
        val monthTotals = mutableListOf<Double>()

        // Six months, oldest to newest.
        calendar.add(Calendar.MONTH, -5)

        repeat(6) {
            val monthStart = calendar.clone() as Calendar

            monthStart.set(Calendar.DAY_OF_MONTH, 1)
            monthStart.set(Calendar.HOUR_OF_DAY, 0)
            monthStart.set(Calendar.MINUTE, 0)
            monthStart.set(Calendar.SECOND, 0)
            monthStart.set(Calendar.MILLISECOND, 0)

            val nextMonth = monthStart.clone() as Calendar
            nextMonth.add(Calendar.MONTH, 1)

            val total = transactions
                .filter { record ->
                    record.type.equals(
                        "Expense",
                        ignoreCase = true
                    ) && record.date?.let { date ->
                        !date.before(monthStart.time) &&
                                date.before(nextMonth.time)
                    } == true
                }
                .sumOf { it.amount }

            monthLabels.add(
                SimpleDateFormat(
                    "MMM",
                    Locale.getDefault()
                ).format(monthStart.time)
            )

            monthTotals.add(total)
            calendar.add(Calendar.MONTH, 1)
        }

        if (monthTotals.none { it > 0 }) {
            monthlyReportChart.clear()
            monthlyReportChart.visibility = View.GONE
            txtMonthlyEmpty.visibility = View.VISIBLE
            return
        }

        txtMonthlyEmpty.visibility = View.GONE
        monthlyReportChart.visibility = View.VISIBLE

        val entries = monthTotals.mapIndexed { index, amount ->
            BarEntry(index.toFloat(), amount.toFloat())
        }

        val dataSet = BarDataSet(
            entries,
            "Monthly Expenses"
        ).apply {
            color = Color.rgb(46, 139, 99)
            valueTextColor = Color.rgb(23, 61, 50)
            valueTextSize = 10f
            setDrawValues(true)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 1000) {
                        "₱" + String.format(
                            Locale.getDefault(),
                            "%.1fk",
                            value / 1000f
                        )
                    } else {
                        "₱" + String.format(
                            Locale.getDefault(),
                            "%.0f",
                            value
                        )
                    }
                }
            }
        }

        monthlyReportChart.data = BarData(dataSet).apply {
            barWidth = 0.55f
        }

        monthlyReportChart.xAxis.valueFormatter =
            IndexAxisValueFormatter(monthLabels)

        monthlyReportChart.xAxis.axisMinimum = -0.5f
        monthlyReportChart.xAxis.axisMaximum = 5.5f

        monthlyReportChart.animateY(700)
        monthlyReportChart.invalidate()
    }

    private fun formatMoney(amount: Double): String {
        return "₱" + String.format(
            Locale.getDefault(),
            "%,.2f",
            amount
        )
    }
}