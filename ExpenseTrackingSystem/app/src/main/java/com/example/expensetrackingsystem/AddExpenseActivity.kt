
package com.example.expensetrackingsystem

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_add_expense)

        val descriptionInput =
            findViewById<EditText>(R.id.descriptionInput)

        val amountInput =
            findViewById<EditText>(R.id.amountInput)

        val typeSpinner =
            findViewById<Spinner>(R.id.typeSpinner)

        val categorySpinner =
            findViewById<Spinner>(R.id.categorySpinner)

        val types = arrayOf("Expense", "Income")

        val categories = arrayOf(
            "Food",
            "Transportation",
            "Shopping",
            "Bills",
            "Education",
            "Health",
            "Entertainment",
            "Others"
        )

        typeSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            types
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        categorySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        findViewById<Button>(R.id.btnSaveTransaction)
            .setOnClickListener {

                if (currentEmail.isBlank()) {
                    Toast.makeText(
                        this,
                        "No logged-in account found. Please log in again.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val description =
                    descriptionInput.text.toString().trim()

                val amountText =
                    amountInput.text.toString().trim()

                val amount = amountText.toDoubleOrNull()

                if (description.isEmpty()) {
                    descriptionInput.error =
                        "Please enter a description"
                    descriptionInput.requestFocus()
                    return@setOnClickListener
                }

                if (
                    amount == null ||
                    !amount.isFinite() ||
                    amount <= 0.0
                ) {
                    amountInput.error =
                        "Enter an amount greater than zero"
                    amountInput.requestFocus()
                    return@setOnClickListener
                }

                val type = typeSpinner.selectedItem.toString()
                val category =
                    categorySpinner.selectedItem.toString()

                val transactions = try {
                    JSONArray(
                        prefs.getString(storageKey, "[]") ?: "[]"
                    )
                } catch (_: Exception) {
                    Toast.makeText(
                        this,
                        "Could not read saved transactions.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                val date = SimpleDateFormat(
                    "MMM dd, yyyy",
                    Locale.ENGLISH
                ).format(Date())

                val transaction = JSONObject().apply {
                    put("description", description)
                    put("amount", amount)
                    put("type", type)
                    put("category", category)
                    put("date", date)
                }

                transactions.put(transaction)

                val saved = prefs.edit()
                    .putString(storageKey, transactions.toString())
                    .commit()

                if (saved) {
                    Toast.makeText(
                        this,
                        "Transaction saved successfully!",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Unable to save transaction. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        findViewById<Button>(R.id.btnCancelTransaction)
            .setOnClickListener {
                finish()
            }
    }
}