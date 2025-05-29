package com.example.hudormplacementapk

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class PlaceAction(context: Context) : SQLiteOpenHelper(context, "DormDB", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        Log.d("PlaceAction", "onCreate: Starting")
        try {
            Log.d("PlaceAction", "onCreate: Dropping tables")
//            db.execSQL("DROP TABLE IF EXISTS students")
//            db.execSQL("DROP TABLE IF EXISTS dorms")
//            db.execSQL("DROP TABLE IF EXISTS building")

            Log.d("PlaceAction", "onCreate: Creating students table")
            db.execSQL(
                """
                CREATE TABLE students (
                    student_id TEXT PRIMARY KEY,
                    stdname TEXT NOT NULL,
                    department TEXT NOT NULL,
                    year INTEGER NOT NULL,
                    gender TEXT CHECK (gender IN ('M', 'F')) NOT NULL,
                    specialcase BOOLEAN NOT NULL,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL,
                    dorm_number TEXT
                )
                """
            )
            Log.d("PlaceAction", "onCreate: Students table created")

            Log.d("PlaceAction", "onCreate: Creating building table")
            db.execSQL(
                """
                CREATE TABLE building (
                    bld_name TEXT PRIMARY KEY,
                    gender TEXT CHECK (gender IN ('M', 'F')) NOT NULL,
                    number_of_dorms INTEGER NOT NULL,
                    capacity_per_dorm INTEGER NOT NULL
                )
                """
            )
            Log.d("PlaceAction", "onCreate: Building table created")

            Log.d("PlaceAction", "onCreate: Creating dorms table")
            db.execSQL(
                """
                CREATE TABLE dorms (
                    dorm_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    dorm_number TEXT NOT NULL UNIQUE,
                    bld_name TEXT NOT NULL,
                    capacity INTEGER NOT NULL,
                    FOREIGN KEY (bld_name) REFERENCES building(bld_name)
                )
                """
            )
            Log.d("PlaceAction", "onCreate: Dorms table created")

            Log.d("PlaceAction", "onCreate: Inserting building data")
            db.execSQL(
                """
                INSERT INTO building (bld_name, gender, number_of_dorms, capacity_per_dorm) VALUES
                    ('Alpha', 'M', 100, 4),
                    ('Beta', 'F', 100, 4),
                    ('Gamma', 'F', 150, 6),
                    ('Sigma', 'M', 150, 6),
                    ('Teta', 'M', 100, 4)
                """
            )
            Log.d("PlaceAction", "onCreate: Building data inserted")
        } catch (e: Exception) {
            Log.e("PlaceAction", "onCreate failed: ${e.message}, StackTrace: ${e.stackTraceToString()}")
            throw e
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.d("PlaceAction", "onUpgrade: oldVersion=$oldVersion, newVersion=$newVersion")
        db.execSQL("DROP TABLE IF EXISTS students")
        db.execSQL("DROP TABLE IF EXISTS dorms")
        db.execSQL("DROP TABLE IF EXISTS building")
        onCreate(db)
    }

    fun assignStudents() {
        Log.d("PlaceAction", "assignStudents: Starting")
        val db = writableDatabase
        try {
            val students = db.rawQuery(
                """
                SELECT student_id, gender, specialcase FROM students 
                WHERE dorm_number IS NULL 
                ORDER BY specialcase DESC, gender, year, department
                """, null
            )

            var studentCount = 0
            while (students.moveToNext()) {
                studentCount++
                val id = students.getString(students.getColumnIndexOrThrow("student_id"))
                val gender = students.getString(students.getColumnIndexOrThrow("gender"))
                val specialCase = students.getInt(students.getColumnIndexOrThrow("specialcase")) == 1
                Log.d("PlaceAction", "assignStudents: Processing student ID=$id, Gender=$gender, SpecialCase=$specialCase")

                val dorm = findOrCreateDorm(db, gender, specialCase)

                dorm?.let {
                    db.execSQL("UPDATE students SET dorm_number = ? WHERE student_id = ?", arrayOf(it, id))
                    db.execSQL("UPDATE dorms SET capacity = capacity - 1 WHERE dorm_number = ?", arrayOf(it))
                    Log.d("Assign", "Student $id assigned to dorm $it")
                } ?: Log.e("Assign", "No dorm available for student $id")
            }
            Log.d("PlaceAction", "assignStudents: Processed $studentCount students")
            students.close()
        } catch (e: Exception) {
            Log.e("PlaceAction", "assignStudents failed: ${e.message}")
        } finally {
            db.close()
        }
    }

    private fun findOrCreateDorm(db: SQLiteDatabase, gender: String, specialCase: Boolean): String? {
        Log.d("PlaceAction", "findOrCreateDorm: Gender=$gender, SpecialCase=$specialCase")
        try {
            val buildings = db.rawQuery(
                "SELECT bld_name, capacity_per_dorm FROM building WHERE gender = ?",
                arrayOf(gender)
            )

            while (buildings.moveToNext()) {
                val bldName = buildings.getString(0)
                val capPerDorm = buildings.getInt(1)
                Log.d("PlaceAction", "findOrCreateDorm: Checking building $bldName")

                val dormsQuery = if (specialCase) {
                    "SELECT dorm_number, dorm_id FROM dorms WHERE bld_name = ? AND capacity > 0 AND dorm_id <= 20 ORDER BY dorm_id LIMIT 1"
                } else {
                    "SELECT dorm_number, dorm_id FROM dorms WHERE bld_name = ? AND capacity > 0 ORDER BY dorm_id LIMIT 1"
                }

                val dorms = db.rawQuery(dormsQuery, arrayOf(bldName))
                if (dorms.moveToFirst()) {
                    val dormNumber = dorms.getString(0)
                    Log.d("PlaceAction", "findOrCreateDorm: Found dorm $dormNumber")
                    dorms.close()
                    buildings.close()
                    return dormNumber
                }
                dorms.close()
                Log.d("PlaceAction", "findOrCreateDorm: No dorm in $bldName, creating")

                val maxDormCursor = db.rawQuery(
                    "SELECT MAX(dorm_id) FROM dorms WHERE bld_name = ?",
                    arrayOf(bldName)
                )

                val nextId = if (maxDormCursor.moveToFirst() && !maxDormCursor.isNull(0)) {
                    maxDormCursor.getInt(0) + 1
                } else {
                    1
                }
                maxDormCursor.close()
                Log.d("PlaceAction", "findOrCreateDorm: Next dorm_id for $bldName is $nextId")

                if (specialCase && nextId > 20) {
                    Log.d("PlaceAction", "findOrCreateDorm: Skipping $bldName for special case")
                    continue
                }

                val newDormNumber = "$bldName-$nextId"
                Log.d("PlaceAction", "findOrCreateDorm: Creating dorm $newDormNumber")

                db.execSQL(
                    "INSERT INTO dorms (dorm_number, bld_name, capacity) VALUES (?, ?, ?)",
                    arrayOf(newDormNumber, bldName, capPerDorm.toString())
                )
                Log.d("PlaceAction", "findOrCreateDorm: Created dorm $newDormNumber")

                buildings.close()
                return newDormNumber
            }

            buildings.close()
            Log.e("PlaceAction", "findOrCreateDorm: No building for Gender=$gender")
            return null
        } catch (e: Exception) {
            Log.e("PlaceAction", "findOrCreateDorm failed: ${e.message}")
            return null
        }
    }

    fun importStudentsFromExcel(context: Context, dbHelper: SQLiteOpenHelper) {
        Log.d("PlaceAction", "importStudentsFromExcel: Starting")
        val db = dbHelper.writableDatabase
        try {
            Log.d("PlaceAction", "Opening students.xlsx")
            val inputStream: InputStream = context.assets.open("students.xlsx")
            Log.d("PlaceAction", "Creating workbook")
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            Log.d("PlaceAction", "Sheet accessed, rows=${sheet.lastRowNum}")

            db.beginTransaction()
            try {
                var rowCount = 0
                for (row in 0..sheet.lastRowNum) {
                    val r = sheet.getRow(row)
                    if (r == null) {
                        Log.d("ExcelImport", "Row $row is null, stopping import")
                        break
                    }
                    // Get student_id, handle numeric or string
                    val studentId = try {
                        r.getCell(0)?.let {
                            when (it.cellType) {
                                org.apache.poi.ss.usermodel.CellType.NUMERIC -> it.numericCellValue.toInt().toString()
                                org.apache.poi.ss.usermodel.CellType.STRING -> it.stringCellValue.trim()
                                else -> null
                            }
                        }
                    } catch (e: Exception) { null }
                    if (studentId.isNullOrEmpty()) {
                        Log.d("ExcelImport", "Row $row has empty student_id, stopping import")
                        break
                    }
                    // Get other fields
                    val stdname = try { r.getCell(1)?.stringCellValue?.trim() } catch (e: Exception) { null }
                    val department = try { r.getCell(2)?.stringCellValue?.trim() } catch (e: Exception) { null }
                    val year = try { r.getCell(3)?.numericCellValue?.toInt() } catch (e: Exception) { null }
                    val gender = try { r.getCell(4)?.stringCellValue?.trim() } catch (e: Exception) { null }
                    val specialcase = try { r.getCell(5)?.numericCellValue?.toInt() == 1 } catch (e: Exception) { false }
                    val username = try { r.getCell(6)?.stringCellValue?.trim() } catch (e: Exception) { null }
                    val password = try { r.getCell(7)?.stringCellValue?.trim() } catch (e: Exception) { null }

                    if (stdname.isNullOrEmpty() || department.isNullOrEmpty() || year == null ||
                        gender.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty()) {
                        Log.w("ExcelImport", "Row $row invalid: ID=$studentId, Name=$stdname, Dept=$department, Year=$year, Gender=$gender, Username=$username")
                        continue
                    }

                    Log.d("ExcelImport", "Importing row $row: ID=$studentId, $stdname, $department, $year, $gender, $specialcase, $username")
                    val stmt = db.compileStatement(
                        "INSERT INTO students (student_id, stdname, department, year, gender, specialcase, username, password) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                    )
                    stmt.bindString(1, studentId)
                    stmt.bindString(2, stdname)
                    stmt.bindString(3, department)
                    stmt.bindLong(4, year.toLong())
                    stmt.bindString(5, gender)
                    stmt.bindLong(6, if (specialcase) 1 else 0)
                    stmt.bindString(7, username)
                    stmt.bindString(8, password)
                    stmt.executeInsert()
                    rowCount++
                }
                db.setTransactionSuccessful()
                Log.d("PlaceAction", "Imported $rowCount rows")
            } catch (e: Exception) {
                Log.e("PlaceAction", "Import error: ${e.message}, StackTrace: ${e.stackTraceToString()}")
            } finally {
                db.endTransaction()
                workbook.close()
                inputStream.close()
            }
            Log.d("PlaceAction", "Inserting test user")
            db.execSQL(
                "INSERT OR IGNORE INTO students (student_id, stdname, department, year, gender, specialcase, username, password, dorm_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf("S9999", "Test User", "CS", 1, "M", 0, "testuser", "password123", "Alpha-1")
            )
            Log.d("PlaceAction", "Test user inserted")
        } catch (e: Exception) {
            Log.e("PlaceAction", "Excel error: ${e.message}, StackTrace: ${e.stackTraceToString()}")
        } finally {
            //db.close()
        }
    }
}