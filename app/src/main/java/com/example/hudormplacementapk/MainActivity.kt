package com.example.hudormplacementapk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.details)

        // Initialize database
        val dbHelper = PlaceAction(this)
        try {
            val db = dbHelper.writableDatabase
            Log.d("MainActivity", "Database initialized: ${db.path}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Database error: ${e.message}")
        }

        // Receive student data from AuthActivity
        val studentId = intent.getStringExtra("student_id") ?: "N/A"
        val studentName = intent.getStringExtra("student_name") ?: "N/A"
        val department = intent.getStringExtra("department") ?: "N/A"
        val year = intent.getStringExtra("year") ?: "N/A"
        val dormNumber = intent.getStringExtra("dorm_number") ?: "N/A"
        val buildingName = intent.getStringExtra("building_name") ?: "N/A"

        // Set TextViews
        findViewById<TextView>(R.id.name).text = studentName
        findViewById<TextView>(R.id.sid).text = studentId
        findViewById<TextView>(R.id.department).text = department
        findViewById<TextView>(R.id.year).text = year
        findViewById<TextView>(R.id.dormid).text = dormNumber

        // Account button (redirect to ProfileActivity)
        findViewById<Button>(R.id.account).setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("student_id", studentId)
                putExtra("student_name", studentName)
                putExtra("department", department)
                putExtra("year", year)
                putExtra("dorm_number", dormNumber)
                putExtra("building_name", buildingName)
            }
            startActivity(intent)
        }

        // Logout button
        findViewById<Button>(R.id.logout).setOnClickListener {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }
}