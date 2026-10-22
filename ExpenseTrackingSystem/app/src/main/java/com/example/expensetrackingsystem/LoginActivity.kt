package com.example.expensetrackingsystem

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.util.Locale

class LoginActivity : AppCompatActivity() {

    private val accountsPrefs by lazy {
        getSharedPreferences("ExpenseTrackerAccounts", MODE_PRIVATE)
    }

    private val sessionPrefs by lazy {
        getSharedPreferences("ExpenseTrackerSession", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isLoggedIn = sessionPrefs.getBoolean("loggedIn", false)
        val currentEmail = sessionPrefs.getString("currentUserEmail", null)

        if (isLoggedIn && !currentEmail.isNullOrBlank()) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.emailInput)
        val password = findViewById<EditText>(R.id.passwordInput)

        findViewById<Button>(R.id.loginButton).setOnClickListener {

            val enteredEmail = email.text.toString().trim()
            val enteredPassword = password.text.toString()

            if (enteredEmail.isBlank() || enteredPassword.isBlank()) {
                showMessage("Fill in all fields")
                return@setOnClickListener
            }

            val accounts = loadAccounts()
            val emailKey = enteredEmail.lowercase(Locale.ROOT)
            val account = accounts.optJSONObject(emailKey)

            if (account == null) {
                showMessage("Account not found. Please sign up first.")
                return@setOnClickListener
            }

            val savedPassword = account.optString("password", "")

            if (enteredPassword != savedPassword) {
                showMessage("Invalid email or password")
                return@setOnClickListener
            }

            // Save only the active login session here.
            // Do not overwrite or delete the registered account.
            sessionPrefs.edit()
                .putBoolean("loggedIn", true)
                .putString("currentUserEmail", emailKey)
                .apply()

            openDashboard()
        }

        findViewById<TextView>(R.id.signUpLink).setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
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

    private fun openDashboard() {
        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}