package com.example.hudormplacementapk

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: SQLiteOpenHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile)

        dbHelper = object : SQLiteOpenHelper(this, "DormDB", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                // No-op: Database created by PlaceAction
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                // No-op
            }
        }

        // Receive student data from MainActivity
        val studentId = intent.getStringExtra("student_id") ?: "N/A"
        val username = getUsername(studentId)

        // Set username field
        val usernameInput = findViewById<TextInputEditText>(R.id.nusername)
        usernameInput.setText(username)

        // Submit button to update username and password
        findViewById<Button>(R.id.button2).setOnClickListener {
            val newUsername = usernameInput.text.toString().trim()
            val newPassword = findViewById<TextInputEditText>(R.id.npassword).text.toString().trim()
            if (newUsername.isEmpty() && newPassword.isEmpty()) {
                Toast.makeText(this, "Enter new username or password", Toast.LENGTH_SHORT).show()
            } else {
                updateProfile(studentId, newUsername, newPassword)
            }
        }

        // Icon button (placeholder action)
        findViewById<ImageButton>(R.id.iconButton).setOnClickListener {
            Toast.makeText(this, "Language change not implemented", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUsername(studentId: String): String {
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.rawQuery(
                "SELECT username FROM students WHERE student_id = ?",
                arrayOf(studentId)
            )
            return if (cursor.moveToFirst()) {
                val username = cursor.getString(0) ?: "N/A"
                cursor.close()
                username
            } else {
                cursor.close()
                "N/A"
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileError", "Failed to fetch username: ${e.message}")
            return "N/A"
        }
        // db.close() // Commented out for database inspection
    }

    private fun updateProfile(studentId: String, newUsername: String, newPassword: String) {
        val db = dbHelper.writableDatabase
        try {
            val values = android.content.ContentValues().apply {
                if (newUsername.isNotEmpty()) put("username", newUsername)
                if (newPassword.isNotEmpty()) put("password", newPassword)
            }
            if (values.size() > 0) {
                val rows = db.update("students", values, "student_id = ?", arrayOf(studentId))
                if (rows > 0) {
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileError", "Failed to update profile: ${e.message}")
            Toast.makeText(this, "Error updating profile", Toast.LENGTH_SHORT).show()
        }
        // db.close() // Commented out for database inspection
    }
}