
package com.example.expensetrackingsystem

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class TransactionActivity : AppCompatActivity() {

    private lateinit var transactionContainer: LinearLayout

    private val currentEmail by lazy {
        getSharedPreferences("ExpenseTrackerSession", MODE_PRIVATE)
            .getString("currentUserEmail", "")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
    }

    private val prefs by lazy {
        getSharedPreferences(
            "ExpenseTrackerData_$currentEmail",
            MODE_PRIVATE
        )
    }

    private val storageKey = "transactions_data"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        transactionContainer =
            findViewById(R.id.transactionContainer)

        findViewById<Button>(R.id.btnAddTransaction)
            .setOnClickListener {
                startActivity(
                    Intent(this, AddExpenseActivity::class.java)
                )
            }

        findViewById<Button>(R.id.btnBackDashboard)
            .setOnClickListener {
                finish()
            }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    private fun loadTransactions() {
        transactionContainer.removeAllViews()
        val transactions = getTransactions()

        if (transactions.length() == 0) {
            val emptyText = TextView(this).apply {
                text = "No transactions yet.\nTap + Add Transaction to begin."
                textSize = 16f
                setTextColor(android.graphics.Color.DKGRAY)
                gravity = Gravity.CENTER
                setPadding(16, 40, 16, 40)
            }

            transactionContainer.addView(emptyText)
            return
        }

        for (i in transactions.length() - 1 downTo 0) {
            val item = transactions.optJSONObject(i) ?: continue

            val description =
                item.optString("description", "Transaction")
            val amount = item.optDouble("amount", 0.0)
            val type = item.optString("type", "Expense")
            val category = item.optString("category", "Others")
            val date = item.optString("date", "")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 14, 18, 14)
                setBackgroundColor(android.graphics.Color.WHITE)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 14)
                layoutParams = params
            }

            val title = TextView(this).apply {
                text = description
                textSize = 18f
                setTextColor(android.graphics.Color.BLACK)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val details = TextView(this).apply {
                text = "$category • $type\n$date"
                textSize = 13f
                setTextColor(android.graphics.Color.DKGRAY)
                setPadding(0, 6, 0, 6)
            }

            val formattedAmount = NumberFormat
                .getCurrencyInstance(Locale("en", "PH"))
                .format(amount)

            val isIncome = type.equals("Income", ignoreCase = true)

            val amountText = TextView(this).apply {
                text = if (isIncome) {
                    "+ $formattedAmount"
                } else {
                    "- $formattedAmount"
                }

                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(
                    if (isIncome) {
                        android.graphics.Color.rgb(34, 139, 34)
                    } else {
                        android.graphics.Color.rgb(210, 50, 50)
                    }
                )
            }

            val deleteButton = Button(this).apply {
                text = "Delete"

                setOnClickListener {
                    AlertDialog.Builder(this@TransactionActivity)
                        .setTitle("Delete Transaction")
                        .setMessage(
                            "Are you sure you want to delete this transaction?"
                        )
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            deleteTransaction(i)
                            loadTransactions()

                            Toast.makeText(
                                this@TransactionActivity,
                                "Transaction deleted",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .show()
                }
            }

            card.addView(title)
            card.addView(details)
            card.addView(amountText)
            card.addView(deleteButton)
            transactionContainer.addView(card)
        }
    }

    private fun getTransactions(): JSONArray {
        val savedData = prefs.getString(storageKey, "[]") ?: "[]"

        return try {
            JSONArray(savedData)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun deleteTransaction(index: Int) {
        val oldTransactions = getTransactions()
        val newTransactions = JSONArray()

        for (i in 0 until oldTransactions.length()) {
            if (i != index) {
                newTransactions.put(oldTransactions.getJSONObject(i))
            }
        }

        prefs.edit()
            .putString(storageKey, newTransactions.toString())
            .apply()
    }
}