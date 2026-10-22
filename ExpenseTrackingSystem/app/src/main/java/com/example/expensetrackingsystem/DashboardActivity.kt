
package com.example.expensetrackingsystem

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import org.json.JSONArray
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var txtBalance: TextView
    private lateinit var txtIncome: TextView
    private lateinit var txtExpenses: TextView
    private lateinit var txtFoodBudget: TextView
    private lateinit var txtTransportBudget: TextView
    private lateinit var txtFoodBudgetAlert: TextView
    private lateinit var txtTransportBudgetAlert: TextView
    private lateinit var txtHighSpendingAlert: TextView
    private lateinit var progressFood: ProgressBar
    private lateinit var progressTransport: ProgressBar
    private lateinit var pieChart: PieChart
    private lateinit var lineChart: LineChart
    private lateinit var txtTransaction1: TextView
    private lateinit var txtTransaction2: TextView
    private lateinit var txtTransaction3: TextView

    private val currentEmail by lazy {
        getSharedPreferences("ExpenseTrackerSession", MODE_PRIVATE)
            .getString("currentUserEmail", "")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
    }

    private val dataPrefs by lazy {
        getSharedPreferences(
            "ExpenseTrackerData_$currentEmail",
            MODE_PRIVATE
        )
    }

    private val budgetPrefs by lazy {
        getSharedPreferences(
            "ExpenseTrackerBudgets_$currentEmail",
            MODE_PRIVATE
        )
    }

    private val dateFormats = listOf(
        "MMM dd, yyyy",
        "MMM d, yyyy",
        "MM/dd/yyyy",
        "M/d/yyyy",
        "dd/MM/yyyy",
        "d/M/yyyy",
        "yyyy-MM-dd",
        "yyyy-MM-dd HH:mm:ss",
        "MMM dd, yyyy HH:mm:ss"
    )

    private data class Transaction(
        val description: String,
        val amount: Double,
        val type: String,
        val category: String,
        val date: String
    )

    private data class SpendingIncrease(
        val category: String,
        val currentAmount: Double,
        val previousAmount: Double,
        val increaseAmount: Double,
        val increasePercent: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        initializeViews()
        setupNavigation()
        setupBudgetEditing()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()

        if (::txtFoodBudget.isInitialized) {
            refreshDashboard()
        }
    }

    private fun initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        txtBalance = findViewById(R.id.txtBalance)
        txtIncome = findViewById(R.id.txtIncome)
        txtExpenses = findViewById(R.id.txtExpenses)

        txtFoodBudget = findViewById(R.id.txtFoodBudget)
        txtTransportBudget = findViewById(R.id.txtTransportBudget)

        txtFoodBudgetAlert = findViewById(R.id.txtFoodBudgetAlert)
        txtTransportBudgetAlert =
            findViewById(R.id.txtTransportBudgetAlert)

        txtHighSpendingAlert =
            findViewById(R.id.txtHighSpendingAlert)

        progressFood = findViewById(R.id.progressFood)
        progressTransport = findViewById(R.id.progressTransport)

        pieChart = findViewById(R.id.pieChart)
        lineChart = findViewById(R.id.lineChart)

        txtTransaction1 = findViewById(R.id.txtTransaction1)
        txtTransaction2 = findViewById(R.id.txtTransaction2)
        txtTransaction3 = findViewById(R.id.txtTransaction3)
    }

    private fun setupNavigation() {
        findViewById<Button>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<View>(R.id.navDashboard).setOnClickListener {
            drawerLayout.closeDrawers()
            refreshDashboard()
        }

        findViewById<View>(R.id.navAddExpense).setOnClickListener {
            openScreen(AddExpenseActivity::class.java)
        }

        findViewById<View>(R.id.navTransactions).setOnClickListener {
            openScreen(TransactionActivity::class.java)
        }

        findViewById<View>(R.id.navAnalytics).setOnClickListener {
            openScreen(AnalyticsActivity::class.java)
        }

        findViewById<View>(R.id.navProfile).setOnClickListener {
            openScreen(ProfileActivity::class.java)
        }
    }

    private fun openScreen(screen: Class<*>) {
        drawerLayout.closeDrawers()
        startActivity(Intent(this, screen))
    }

    private fun setupBudgetEditing() {
        txtFoodBudget.setOnClickListener {
            showEditBudgetDialog(
                "Food & Groceries Budget",
                "food_budget",
                5000.0
            )
        }

        txtTransportBudget.setOnClickListener {
            showEditBudgetDialog(
                "Transportation Budget",
                "transport_budget",
                2000.0
            )
        }
    }

    private fun showEditBudgetDialog(
        title: String,
        key: String,
        defaultBudget: Double
    ) {
        val input = EditText(this)

        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        val currentBudget = budgetPrefs.getFloat(
            key,
            defaultBudget.toFloat()
        ).toDouble()

        input.setText(currentBudget.toString())
        input.selectAll()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Enter your monthly budget:")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val newBudget = input.text.toString().toDoubleOrNull()

                if (
                    newBudget == null ||
                    !newBudget.isFinite() ||
                    newBudget <= 0.0
                ) {
                    Toast.makeText(
                        this,
                        "Enter a valid budget greater than zero.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    budgetPrefs.edit()
                        .putFloat(key, newBudget.toFloat())
                        .apply()

                    refreshDashboard()

                    Toast.makeText(
                        this,
                        "Budget updated successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    private fun refreshDashboard() {
        if (currentEmail.isBlank()) {
            txtBalance.text = formatPeso(0.0)
            txtIncome.text = formatPeso(0.0)
            txtExpenses.text = formatPeso(0.0)

            updateBudgets(emptyList())
            updateRecentTransactions(emptyList())
            updatePieChart(emptyList())
            updateLineChart(emptyList())

            txtHighSpendingAlert.text =
                "Sign in to view your spending alerts."

            txtHighSpendingAlert.setTextColor(Color.DKGRAY)
            txtHighSpendingAlert.setBackgroundColor(Color.LTGRAY)
            txtHighSpendingAlert.visibility = View.VISIBLE
            return
        }

        val transactions = loadTransactions()

        val totalIncome = transactions
            .filter {
                it.type.equals("Income", ignoreCase = true)
            }
            .sumOf { it.amount }

        val totalExpenses = transactions
            .filter {
                it.type.equals("Expense", ignoreCase = true)
            }
            .sumOf { it.amount }

        val balance = totalIncome - totalExpenses

        txtBalance.text = formatPeso(balance)
        txtIncome.text = formatPeso(totalIncome)
        txtExpenses.text = formatPeso(totalExpenses)

        updateBudgets(transactions)
        updateRecentTransactions(transactions)
        updatePieChart(transactions)
        updateLineChart(transactions)
        updateHighSpendingAlert(transactions)
    }

    private fun loadTransactions(): List<Transaction> {
        val jsonString = dataPrefs.getString(
            "transactions_data",
            "[]"
        ) ?: "[]"

        val result = mutableListOf<Transaction>()

        try {
            val array = JSONArray(jsonString)

            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue

                result.add(
                    Transaction(
                        description = item.optString(
                            "description",
                            "Transaction"
                        ),
                        amount = item.optDouble("amount", 0.0),
                        type = item.optString("type", "Expense"),
                        category = item.optString("category", ""),
                        date = item.optString("date", "")
                    )
                )
            }
        } catch (_: Exception) {
            Toast.makeText(
                this,
                "Unable to read transactions.",
                Toast.LENGTH_SHORT
            ).show()
        }

        return result
    }

    private fun updateBudgets(transactions: List<Transaction>) {
        val foodBudget = budgetPrefs.getFloat(
            "food_budget",
            5000f
        ).toDouble()

        val transportBudget = budgetPrefs.getFloat(
            "transport_budget",
            2000f
        ).toDouble()

        var foodSpent = 0.0
        var transportSpent = 0.0

        for (transaction in transactions) {
            if (!transaction.type.equals("Expense", ignoreCase = true)) {
                continue
            }

            if (!isCurrentMonth(transaction.date)) {
                continue
            }

            val category = transaction.category
                .trim()
                .lowercase(Locale.ROOT)

            when {
                category.contains("food") ||
                        category.contains("grocer") -> {
                    foodSpent += transaction.amount
                }

                category.contains("transport") ||
                        category.contains("commute") ||
                        category.contains("fare") -> {
                    transportSpent += transaction.amount
                }
            }
        }

        txtFoodBudget.text =
            "${formatPeso(foodSpent)} of ${formatPeso(foodBudget)}"

        txtTransportBudget.text =
            "${formatPeso(transportSpent)} of ${formatPeso(transportBudget)}"

        progressFood.max = 100
        progressTransport.max = 100

        progressFood.progress = percentageProgress(foodSpent, foodBudget)
        progressTransport.progress =
            percentageProgress(transportSpent, transportBudget)

        showBudgetAlert(
            txtFoodBudgetAlert,
            foodSpent,
            foodBudget,
            "Food & Groceries"
        )

        showBudgetAlert(
            txtTransportBudgetAlert,
            transportSpent,
            transportBudget,
            "Transportation"
        )
    }

    private fun percentageProgress(
        spent: Double,
        budget: Double
    ): Int {
        if (budget <= 0.0) return 0

        return ((spent / budget) * 100.0)
            .toInt()
            .coerceIn(0, 100)
    }

    private fun showBudgetAlert(
        alertView: TextView,
        spent: Double,
        budget: Double,
        category: String
    ) {
        alertView.text = ""
        alertView.visibility = View.GONE

        if (budget <= 0.0) return

        val percentage = (spent / budget) * 100.0

        when {
            percentage >= 100.0 -> {
                val exceeded = spent - budget

                alertView.text =
                    "OVER BUDGET! You exceeded your $category budget by " +
                            "${formatPeso(exceeded)}."

                alertView.setTextColor(Color.rgb(180, 35, 24))
                alertView.setBackgroundColor(Color.rgb(252, 231, 231))
                alertView.visibility = View.VISIBLE
            }

            percentage >= 80.0 -> {
                val remaining = budget - spent

                alertView.text =
                    "BUDGET WARNING! ${percentage.toInt()}% used. " +
                            "${formatPeso(remaining)} remaining."

                alertView.setTextColor(Color.rgb(154, 97, 0))
                alertView.setBackgroundColor(Color.rgb(255, 240, 214))
                alertView.visibility = View.VISIBLE
            }
        }

        alertView.alpha = 1f
        alertView.requestLayout()
        alertView.invalidate()
    }

    private fun updateHighSpendingAlert(
        transactions: List<Transaction>
    ) {
        val totalIncome = transactions
            .filter {
                it.type.equals("Income", ignoreCase = true)
            }
            .sumOf { it.amount }

        val totalExpenses = transactions
            .filter {
                it.type.equals("Expense", ignoreCase = true)
            }
            .sumOf { it.amount }

        val balance = totalIncome - totalExpenses
        val isLowBalance = balance <= 0.0

        val today = Calendar.getInstance()

        val currentStart = (today.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentEnd = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val previousStart = (currentStart.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }

        val targetDay = today.get(Calendar.DAY_OF_MONTH)

        val previousEnd = (previousStart.clone() as Calendar).apply {
            set(
                Calendar.DAY_OF_MONTH,
                minOf(
                    targetDay,
                    getActualMaximum(Calendar.DAY_OF_MONTH)
                )
            )
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val previousComparisonStart =
            previousStart.clone() as Calendar

        val currentTotals = mutableMapOf<String, Double>()
        val previousTotals = mutableMapOf<String, Double>()
        val categoryNames = mutableMapOf<String, String>()

        for (transaction in transactions) {
            if (!transaction.type.equals("Expense", ignoreCase = true)) {
                continue
            }

            val date = parseTransactionDate(transaction.date) ?: continue
            val category = transaction.category.trim().ifBlank { "Others" }
            val key = category.lowercase(Locale.ROOT)

            categoryNames[key] = category

            if (
                !date.before(currentStart.time) &&
                date.before(currentEnd.time)
            ) {
                currentTotals[key] =
                    (currentTotals[key] ?: 0.0) + transaction.amount
            }

            if (
                !date.before(previousComparisonStart.time) &&
                date.before(previousEnd.time)
            ) {
                previousTotals[key] =
                    (previousTotals[key] ?: 0.0) + transaction.amount
            }
        }

        val increases = currentTotals.mapNotNull { (key, currentAmount) ->
            val previousAmount = previousTotals[key] ?: 0.0

            if (
                previousAmount > 0.0 &&
                currentAmount >= previousAmount * 1.5 &&
                currentAmount - previousAmount >= 500.0
            ) {
                SpendingIncrease(
                    category = categoryNames[key] ?: key,
                    currentAmount = currentAmount,
                    previousAmount = previousAmount,
                    increaseAmount = currentAmount - previousAmount,
                    increasePercent = (
                            (currentAmount - previousAmount) /
                                    previousAmount * 100.0
                            ).toInt()
                )
            } else {
                null
            }
        }.sortedByDescending { it.increaseAmount }

        val spendingMessage = if (increases.isEmpty()) {
            "SPENDING INSIGHTS\n\n" +
                    "No unusual spending increases detected compared " +
                    "with the same period last month."
        } else {
            buildString {
                append("HIGH SPENDING ALERT\n\n")

                increases.take(3).forEach { item ->
                    append("• ${item.category}: spending increased ")
                    append("${item.increasePercent}% ")
                    append("(${formatPeso(item.increaseAmount)} more).\n")
                    append("  Current: ${formatPeso(item.currentAmount)}\n")
                    append(
                        "  Previous period: " +
                                "${formatPeso(item.previousAmount)}\n\n"
                    )
                }

                append(
                    "Compared with the same elapsed period last month."
                )
            }.trim()
        }

        val lowBalanceMessage = if (isLowBalance) {
            "⚠ LOW BALANCE WARNING\n\n" +
                    "Your current balance is ${formatPeso(balance)}.\n" +
                    "Your expenses are equal to or higher than your income.\n\n"
        } else {
            ""
        }

        txtHighSpendingAlert.text =
            lowBalanceMessage + spendingMessage

        when {
            isLowBalance -> {
                txtHighSpendingAlert.setTextColor(
                    Color.rgb(180, 35, 24)
                )
                txtHighSpendingAlert.setBackgroundColor(
                    Color.rgb(252, 231, 231)
                )
            }

            increases.isNotEmpty() -> {
                txtHighSpendingAlert.setTextColor(
                    Color.rgb(154, 70, 0)
                )
                txtHighSpendingAlert.setBackgroundColor(
                    Color.rgb(255, 240, 214)
                )
            }

            else -> {
                txtHighSpendingAlert.setTextColor(
                    Color.rgb(23, 107, 69)
                )
                txtHighSpendingAlert.setBackgroundColor(
                    Color.rgb(226, 245, 234)
                )
            }
        }

        txtHighSpendingAlert.visibility = View.VISIBLE
        txtHighSpendingAlert.alpha = 1f
        txtHighSpendingAlert.requestLayout()
        txtHighSpendingAlert.invalidate()
    }

    private fun isCurrentMonth(dateText: String): Boolean {
        val transactionDate = parseTransactionDate(dateText) ?: return false

        val today = Calendar.getInstance()
        val transactionCalendar = Calendar.getInstance().apply {
            time = transactionDate
        }

        return today.get(Calendar.YEAR) ==
                transactionCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) ==
                transactionCalendar.get(Calendar.MONTH)
    }

    private fun parseTransactionDate(dateText: String): Date? {
        for (pattern in dateFormats) {
            try {
                val formatter = SimpleDateFormat(
                    pattern,
                    Locale.ENGLISH
                ).apply {
                    isLenient = false
                }

                val position = ParsePosition(0)
                val parsedDate = formatter.parse(dateText, position)

                if (
                    parsedDate != null &&
                    position.index == dateText.length
                ) {
                    return parsedDate
                }
            } catch (_: Exception) {
                // Try the next supported date format.
            }
        }

        return null
    }

    private fun updateRecentTransactions(
        transactions: List<Transaction>
    ) {
        val recent = transactions.asReversed().take(3)

        val views = listOf(
            txtTransaction1,
            txtTransaction2,
            txtTransaction3
        )

        for (i in views.indices) {
            if (i < recent.size) {
                val item = recent[i]
                val sign = if (
                    item.type.equals("Income", ignoreCase = true)
                ) "+" else "-"

                views[i].text =
                    "${item.description}  $sign${formatPeso(item.amount)}"

                views[i].visibility = View.VISIBLE
            } else {
                views[i].text = "No transactions yet"
                views[i].visibility =
                    if (i == 0 && recent.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }
    }

    private fun updatePieChart(transactions: List<Transaction>) {
        val categoryTotals = mutableMapOf<String, Float>()

        for (transaction in transactions) {
            if (
                transaction.type.equals("Expense", ignoreCase = true) &&
                transaction.amount > 0
            ) {
                val category = transaction.category.ifBlank { "Other" }

                categoryTotals[category] =
                    (categoryTotals[category] ?: 0f) +
                            transaction.amount.toFloat()
            }
        }

        val entries = categoryTotals.map { (category, amount) ->
            PieEntry(amount, category)
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No expenses yet")
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Expenses by Category")

        dataSet.colors = listOf(
            Color.rgb(76, 175, 80),
            Color.rgb(33, 150, 243),
            Color.rgb(255, 152, 0),
            Color.rgb(156, 39, 176),
            Color.rgb(244, 67, 54),
            Color.rgb(0, 150, 136)
        )

        dataSet.valueTextSize = 12f

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.setUsePercentValues(true)
        pieChart.centerText = "Expenses"
        pieChart.isDrawHoleEnabled = true
        pieChart.legend.isEnabled = true
        pieChart.invalidate()
    }

    private fun updateLineChart(transactions: List<Transaction>) {
        val dailyTotals = mutableMapOf<String, Float>()

        val formatter = SimpleDateFormat(
            "MMM dd",
            Locale.ENGLISH
        )

        for (transaction in transactions) {
            if (!transaction.type.equals("Expense", ignoreCase = true)) {
                continue
            }

            val date = parseTransactionDate(transaction.date) ?: continue
            val dayLabel = formatter.format(date)

            dailyTotals[dayLabel] =
                (dailyTotals[dayLabel] ?: 0f) +
                        transaction.amount.toFloat()
        }

        val entries = dailyTotals.values.mapIndexed { index, amount ->
            Entry(index.toFloat(), amount)
        }

        if (entries.isEmpty()) {
            lineChart.clear()
            lineChart.setNoDataText("No expense data yet")
            lineChart.invalidate()
            return
        }

        val dataSet = LineDataSet(entries, "Daily Expenses")

        dataSet.color = Color.rgb(76, 175, 80)
        dataSet.setCircleColor(Color.rgb(76, 175, 80))
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 4f
        dataSet.valueTextSize = 10f

        lineChart.data = LineData(dataSet)
        lineChart.description.isEnabled = false
        lineChart.axisRight.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.invalidate()
    }

    private fun formatPeso(amount: Double): String {
        return "₱" + String.format(
            Locale.US,
            "%,.2f",
            amount
        )
    }
}