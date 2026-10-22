package com.example.expensetrackingsystem

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private val accountsPrefs by lazy {
        getSharedPreferences("ExpenseTrackerAccounts", MODE_PRIVATE)
    }

    private val sessionPrefs by lazy {
        getSharedPreferences("ExpenseTrackerSession", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val name = findViewById<EditText>(R.id.nameInput)
        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)
        val confirm = findViewById<EditText>(R.id.confirmPasswordInput)

        findViewById<Button>(R.id.signUpButton).setOnClickListener {

            val n = name.text.toString().trim()
            val e = email.text.toString().trim()
            val p = password.text.toString()
            val confirmPassword = confirm.text.toString()

            when {
                n.isBlank() || e.isBlank() ||
                        p.isBlank() || confirmPassword.isBlank() -> {
                    showMessage("Please complete all fields")
                }

                !Patterns.EMAIL_ADDRESS.matcher(e).matches() -> {
                    showMessage("Enter a valid email")
                }

                p.length < 6 -> {
                    showMessage("Password must have at least 6 characters")
                }

                p != confirmPassword -> {
                    showMessage("Passwords do not match")
                }

                else -> {
                    val accounts = loadAccounts()
                    val emailKey = e.lowercase(Locale.ROOT)

                    if (accounts.has(emailKey)) {
                        showMessage("Account already exists. Please log in.")
                    } else {
                        val account = JSONObject().apply {
                            put("name", n)
                            put("email", e)
                            put("password", p)
                        }

                        accounts.put(emailKey, account)

                        accountsPrefs.edit()
                            .putString("accounts_data", accounts.toString())
                            .apply()

                        // Do not automatically log in as the new account.
                        sessionPrefs.edit()
                            .putBoolean("loggedIn", false)
                            .remove("currentUserEmail")
                            .apply()

                        Toast.makeText(
                            this,
                            "Account created! Please log in.",
                            Toast.LENGTH_LONG
                        ).show()

                        startActivity(
                            Intent(this, LoginActivity::class.java)
                        )
                        finish()
                    }
                }
            }
        }

        findViewById<TextView>(R.id.loginLink).setOnClickListener {
            finish()
        }
    }

    private fun loadAccounts(): JSONObject {
        val savedData = accountsPrefs.getString("accounts_data", null)

        if (!savedData.isNullOrBlank()) {
            return try {
                JSONObject(savedData)
            } catch (_: Exception) {
                JSONObject()
            }
        }

        // Migrate the previous single-account registration, if one exists.
        val oldPrefs = getSharedPreferences("ExpenseTracker", MODE_PRIVATE)
        val oldEmail = oldPrefs.getString("email", "").orEmpty()
        val oldPassword = oldPrefs.getString("password", "").orEmpty()
        val oldName = oldPrefs.getString("name", "").orEmpty()

        val accounts = JSONObject()

        if (oldEmail.isNotBlank() && oldPassword.isNotBlank()) {
            val oldAccount = JSONObject().apply {
                put("name", oldName)
                put("email", oldEmail)
                put("password", oldPassword)
            }

            accounts.put(oldEmail.lowercase(Locale.ROOT), oldAccount)

            accountsPrefs.edit()
                .putString("accounts_data", accounts.toString())
                .apply()
        }

        return accounts
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}