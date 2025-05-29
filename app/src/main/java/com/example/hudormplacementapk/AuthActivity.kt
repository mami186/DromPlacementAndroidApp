package com.example.hudormplacementapk

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class AuthActivity : AppCompatActivity() {

    private lateinit var dbHelper: PlaceAction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = PlaceAction(this)
        try {
            // Initialize database
            val db = dbHelper.writableDatabase
            Log.d("AuthActivity", "Database initialized: ${db.path}")

            // Import students from Excel
            dbHelper.importStudentsFromExcel(this, dbHelper)
            Log.d("AuthActivity", "Students imported from Excel")

            // Assign students to dorms
            dbHelper.assignStudents()
            Log.d("AuthActivity", "Students assigned to dorms")
        } catch (e: Exception) {
            Log.e("AuthActivity", "Database setup failed: ${e.message}")
        }

        val usernameInput = findViewById<TextInputEditText>(R.id.username)
        val passwordInput = findViewById<TextInputEditText>(R.id.password)
        val loginButton = findViewById<Button>(R.id.loginButton)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            } else {
                val studentData = authenticate(username, password)
                if (studentData != null) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("student_id", studentData[0])
                        putExtra("student_name", studentData[1])
                        putExtra("department", studentData[2])
                        putExtra("year", studentData[3])
                        putExtra("dorm_number", studentData[4])
                        putExtra("building_name", studentData[5])
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Debug: Inspect students table
        debugStudentsTable()
    }

    private fun authenticate(username: String, password: String): Array<String?>? {
        val db = dbHelper.readableDatabase
        try {
            val cursor = db.rawQuery(
                """
                SELECT s.student_id, s.stdname, s.department, s.year, s.dorm_number, d.bld_name
                FROM students s
                LEFT JOIN dorms d ON s.dorm_number = d.dorm_number
                WHERE s.username = ? AND s.password = ?
                """,
                arrayOf(username.trim(), password.trim())
            )
            return if (cursor.moveToFirst()) {
                val sid = cursor.getString(cursor.getColumnIndexOrThrow("student_id"))
                Log.d("Auth", "Fetched student_id: $sid")
                val result = arrayOf(
                    sid,
                    cursor.getString(cursor.getColumnIndexOrThrow("stdname")),
                    cursor.getString(cursor.getColumnIndexOrThrow("department")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("year")).toString(),
                    cursor.getString(cursor.getColumnIndexOrThrow("dorm_number")),
                    cursor.getString(cursor.getColumnIndexOrThrow("bld_name"))
                )
                cursor.close()
                result
            } else {
                cursor.close()
                null
            }
        } catch (e: Exception) {
            Log.e("AuthError", "Authentication failed: ${e.message}")
            return null
        } finally {
            db.close()
        }
    }

    private fun debugStudentsTable() {
        val db = dbHelper.readableDatabase
        try {
            // Check if students table exists
            val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='students'", null)
            val tableExists = cursor.moveToFirst()
            cursor.close()
            Log.d("Debug", "Students table exists: $tableExists")
            if (!tableExists) return

            // Log schema
            val schemaCursor = db.rawQuery("PRAGMA table_info(students)", null)
            while (schemaCursor.moveToNext()) {
                val columnName = schemaCursor.getString(1)
                val columnType = schemaCursor.getString(2)
                Log.d("Schema", "Column: $columnName, Type: $columnType")
            }
            schemaCursor.close()

            // Log data
            val dataCursor = db.rawQuery("SELECT student_id, username, stdname, department, year, dorm_number FROM students", null)
            Log.d("Debug", "Student count: ${dataCursor.count}")
            while (dataCursor.moveToNext()) {
                val sid = dataCursor.getString(dataCursor.getColumnIndexOrThrow("student_id"))
                val username = dataCursor.getString(dataCursor.getColumnIndexOrThrow("username"))
                val name = dataCursor.getString(dataCursor.getColumnIndexOrThrow("stdname"))
                val dept = dataCursor.getString(dataCursor.getColumnIndexOrThrow("department"))
                val year = dataCursor.getInt(dataCursor.getColumnIndexOrThrow("year"))
                val dorm = dataCursor.getString(dataCursor.getColumnIndexOrThrow("dorm_number")) ?: "N/A"
                Log.d("Debug", "Student: ID=$sid, Username=$username, Name=$name, Dept=$dept, Year=$year, Dorm=$dorm")
            }
            dataCursor.close()
        } catch (e: Exception) {
            Log.e("DebugError", "Failed to inspect students table: ${e.message}")
        } finally {
            //db.close()
        }
    }
}