
package com.example.expensetrackingsystem

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var imgProfilePicture: ImageView
    private lateinit var txtProfileDisplayName: TextView
    private lateinit var txtProfileDisplayEmail: TextView

    private lateinit var editProfileName: EditText
    private lateinit var editProfileEmail: EditText
    private lateinit var editProfilePhone: EditText
    private lateinit var editProfileBirthDate: EditText
    private lateinit var spinnerProfileGender: Spinner
    private lateinit var editProfileAddress: EditText
    private lateinit var editProfileOccupation: EditText

    private lateinit var btnChangeProfilePhoto: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var btnBackProfile: Button
    private lateinit var btnProfileLogout: Button

    private val sessionPrefs by lazy {
        getSharedPreferences("ExpenseTrackerSession", MODE_PRIVATE)
    }

    private val currentEmail by lazy {
        sessionPrefs.getString("currentUserEmail", "")
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()
    }

    private val profilePrefs by lazy {
        getSharedPreferences(
            "ExpenseTrackerProfile_$currentEmail",
            MODE_PRIVATE
        )
    }

    private val profilePhotoPicker =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            if (uri != null) {
                saveProfilePhoto(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupGenderSpinner()
        setupClickListeners()
        loadProfile()
    }

    private fun initializeViews() {
        imgProfilePicture = findViewById(R.id.imgProfilePicture)

        txtProfileDisplayName =
            findViewById(R.id.txtProfileDisplayName)

        txtProfileDisplayEmail =
            findViewById(R.id.txtProfileDisplayEmail)

        editProfileName = findViewById(R.id.editProfileName)
        editProfileEmail = findViewById(R.id.editProfileEmail)
        editProfilePhone = findViewById(R.id.editProfilePhone)
        editProfileBirthDate = findViewById(R.id.editProfileBirthDate)

        spinnerProfileGender =
            findViewById(R.id.spinnerProfileGender)

        editProfileAddress = findViewById(R.id.editProfileAddress)
        editProfileOccupation = findViewById(R.id.editProfileOccupation)

        btnChangeProfilePhoto =
            findViewById(R.id.btnChangeProfilePhoto)

        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnBackProfile = findViewById(R.id.btnBackProfile)
        btnProfileLogout = findViewById(R.id.btnProfileLogout)
    }

    private fun setupGenderSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.profile_gender_options,
            android.R.layout.simple_spinner_item
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinnerProfileGender.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBackProfile.setOnClickListener {
            finish()
        }

        btnChangeProfilePhoto.setOnClickListener {
            profilePhotoPicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        editProfileBirthDate.setOnClickListener {
            showDatePicker()
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        btnProfileLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun loadProfile() {
        val savedName = profilePrefs.getString(
            "profile_name",
            ""
        ).orEmpty()

        val savedEmail = profilePrefs.getString(
            "profile_email",
            currentEmail
        ).orEmpty()

        editProfileName.setText(savedName)
        editProfileEmail.setText(savedEmail)

        editProfilePhone.setText(
            profilePrefs.getString("phone", "").orEmpty()
        )

        editProfileBirthDate.setText(
            profilePrefs.getString("birth_date", "").orEmpty()
        )

        editProfileAddress.setText(
            profilePrefs.getString("address", "").orEmpty()
        )

        editProfileOccupation.setText(
            profilePrefs.getString("occupation", "").orEmpty()
        )

        val savedGender = profilePrefs.getString(
            "gender",
            "Select gender"
        )

        val genderAdapter = spinnerProfileGender.adapter

        if (genderAdapter != null) {
            for (index in 0 until genderAdapter.count) {
                if (genderAdapter.getItem(index).toString() == savedGender) {
                    spinnerProfileGender.setSelection(index)
                    break
                }
            }
        }

        updateProfileHeader()
        loadProfilePhoto()
    }

    private fun saveProfile() {
        val name = editProfileName.text.toString().trim()
        val email = editProfileEmail.text.toString()
            .trim()
            .lowercase(Locale.ROOT)

        if (name.isEmpty()) {
            editProfileName.error = "Please enter your name"
            editProfileName.requestFocus()
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS
                .matcher(email).matches()
        ) {
            editProfileEmail.error = "Please enter a valid email address"
            editProfileEmail.requestFocus()
            return
        }

        val saved = profilePrefs.edit()
            .putString("profile_name", name)
            .putString("profile_email", email)
            .putString(
                "phone",
                editProfilePhone.text.toString().trim()
            )
            .putString(
                "birth_date",
                editProfileBirthDate.text.toString().trim()
            )
            .putString(
                "gender",
                spinnerProfileGender.selectedItem.toString()
            )
            .putString(
                "address",
                editProfileAddress.text.toString().trim()
            )
            .putString(
                "occupation",
                editProfileOccupation.text.toString().trim()
            )
            .commit()

        if (saved) {
            updateProfileHeader()

            Toast.makeText(
                this,
                "Profile saved successfully!",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Unable to save profile. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun updateProfileHeader() {
        val name = profilePrefs.getString(
            "profile_name",
            ""
        ).orEmpty()

        val email = profilePrefs.getString(
            "profile_email",
            currentEmail
        ).orEmpty()

        txtProfileDisplayName.text =
            if (name.isBlank()) "Your Name" else name

        txtProfileDisplayEmail.text = email
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )

                editProfileBirthDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.datePicker.maxDate =
            System.currentTimeMillis()

        datePicker.show()
    }

    private fun saveProfilePhoto(uri: Uri) {
        var newPhotoFile: File? = null

        try {
            if (currentEmail.isBlank()) {
                Toast.makeText(
                    this,
                    "Please log in again before changing your photo.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream == null) {
                Toast.makeText(
                    this,
                    "Unable to open the selected photo.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Use a new filename for every selected photo.
            // This prevents Android from displaying a cached old image.
            newPhotoFile = File(
                filesDir,
                "profile_${currentEmail.hashCode()}_${System.currentTimeMillis()}.jpg"
            )

            inputStream.use { input ->
                FileOutputStream(newPhotoFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }

            if (!newPhotoFile.exists() || newPhotoFile.length() == 0L) {
                newPhotoFile.delete()

                Toast.makeText(
                    this,
                    "The selected photo could not be saved.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val oldPhotoPath = profilePrefs.getString(
                "profile_photo_path",
                null
            )

            // Save the new path before displaying the photo.
            val saved = profilePrefs.edit()
                .putString(
                    "profile_photo_path",
                    newPhotoFile.absolutePath
                )
                .commit()

            if (!saved) {
                newPhotoFile.delete()

                Toast.makeText(
                    this,
                    "Unable to save the new profile photo.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            // Display the photo from its new, unique file path.
            imgProfilePicture.setImageDrawable(null)
            imgProfilePicture.setImageURI(null)
            imgProfilePicture.setImageURI(
                Uri.fromFile(newPhotoFile)
            )

            // Delete the previous photo only after the new one is saved.
            if (!oldPhotoPath.isNullOrBlank() &&
                oldPhotoPath != newPhotoFile.absolutePath
            ) {
                try {
                    File(oldPhotoPath).delete()
                } catch (_: Exception) {
                    // The new photo is already saved, so continue.
                }
            }

            Toast.makeText(
                this,
                "Profile photo updated successfully!",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            newPhotoFile?.delete()

            Toast.makeText(
                this,
                "Unable to update photo. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadProfilePhoto() {
        val photoPath = profilePrefs.getString(
            "profile_photo_path",
            null
        )

        if (photoPath.isNullOrBlank()) {
            return
        }

        val photoFile = File(photoPath)

        if (photoFile.exists() && photoFile.length() > 0L) {
            imgProfilePicture.setImageDrawable(null)
            imgProfilePicture.setImageURI(null)
            imgProfilePicture.setImageURI(
                Uri.fromFile(photoFile)
            )
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Logout") { _, _ ->
                // Clear only the active session.
                // Do not delete the user's saved profile or transactions.
                sessionPrefs.edit()
                    .putBoolean("loggedIn", false)
                    .remove("currentUserEmail")
                    .apply()

                val intent = Intent(this, LoginActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK

                startActivity(intent)
                finish()
            }
            .show()
    }
}