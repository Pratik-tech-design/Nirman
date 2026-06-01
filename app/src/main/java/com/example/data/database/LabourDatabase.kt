package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.models.Attendance
import com.example.data.models.AttendanceDraft
import com.example.data.models.AttendanceAuditLog
import com.example.data.models.SiteExpense
import com.example.data.models.RecentActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Labour::class, Site::class, Payment::class, UserProfile::class, Attendance::class, AttendanceDraft::class, AttendanceAuditLog::class, SiteExpense::class], version = 9, exportSchema = false)
abstract class LabourDatabase : RoomDatabase() {
    abstract fun labourDao(): LabourDao

    companion object {
        @Volatile
        private var INSTANCE: LabourDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): LabourDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LabourDatabase::class.java,
                    "labour_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun populateDatabase(dao: LabourDao) {
            val seedUserId = "default_contractor_uid"

            // Seed a default user profile first
            dao.insertUserProfile(
                UserProfile(
                    uid = seedUserId,
                    displayName = "Aditya Verma",
                    email = "contractor@test.com",
                    profilePhoto = "",
                    preferredLanguage = "English"
                )
            )

            // Seed default sites
            val site1Id = dao.insertSite(Site(userId = seedUserId, name = "Metro Station Phase 2", location = "Sector 62, Noida", managerName = "R.K. Sharma"))
            val site2Id = dao.insertSite(Site(userId = seedUserId, name = "High-Rise Tower A", location = "Whitefield, Bengaluru", managerName = "Anurag Verma"))
            val site3Id = dao.insertSite(Site(userId = seedUserId, name = "City Bypass Roadwork", location = "NH-53, Raipur", managerName = "S.K. Yadav"))

            // Seed initial labor
            val l1Id = dao.insertLabour(Labour(
                userId = seedUserId,
                name = "Ram Singh",
                phoneNumber = "9876543210",
                skillType = "Mason (राजमिस्त्री)",
                dailyWage = 650.0,
                siteId = site1Id.toInt()
            ))

            val l2Id = dao.insertLabour(Labour(
                userId = seedUserId,
                name = "Shyam Lal",
                phoneNumber = "8765432109",
                skillType = "Helper (मददगार)",
                dailyWage = 400.0,
                siteId = site1Id.toInt()
            ))

            val l3Id = dao.insertLabour(Labour(
                userId = seedUserId,
                name = "Karan Patel",
                phoneNumber = "7654321098",
                skillType = "Welder (वेल्डर)",
                dailyWage = 600.0,
                siteId = site3Id.toInt()
            ))

            val l4Id = dao.insertLabour(Labour(
                userId = seedUserId,
                name = "Amit Kumar",
                phoneNumber = "9123456780",
                skillType = "Electrician (बिजली मिस्त्री)",
                dailyWage = 550.0,
                siteId = site2Id.toInt()
            ))

            // Seed initial payments
            dao.insertPayment(Payment(
                userId = seedUserId,
                labourId = l1Id.toInt(),
                labourName = "Ram Singh",
                siteId = site1Id.toInt(),
                amount = 1300.0,
                date = "2026-05-25",
                paymentMode = "UPI",
                paymentType = "Daily Wage (Kharchi)",
                remarks = "Paid for 2 days wage"
            ))

            dao.insertPayment(Payment(
                userId = seedUserId,
                labourId = l2Id.toInt(),
                labourName = "Shyam Lal",
                siteId = site1Id.toInt(),
                amount = 1400.0,
                date = "2026-05-26",
                paymentMode = "Cash",
                paymentType = "Weekly Advance",
                remarks = "Advance for weekly expenses"
            ))

            dao.insertPayment(Payment(
                userId = seedUserId,
                labourId = l3Id.toInt(),
                labourName = "Karan Patel",
                siteId = site3Id.toInt(),
                amount = 250.0,
                date = "2026-05-26",
                paymentMode = "Cash",
                paymentType = "Deduction",
                remarks = "Safety helmet damage deduction"
            ))

            dao.insertPayment(Payment(
                userId = seedUserId,
                labourId = l4Id.toInt(),
                labourName = "Amit Kumar",
                siteId = site2Id.toInt(),
                amount = 1000.0,
                date = "2026-05-27",
                paymentMode = "Bank Transfer",
                paymentType = "Bonus",
                remarks = "Overtime and holiday completion bonus"
            ))

            dao.insertPayment(Payment(
                userId = seedUserId,
                labourId = l1Id.toInt(),
                labourName = "Ram Singh",
                siteId = site1Id.toInt(),
                amount = 350.0,
                date = "2026-05-27",
                paymentMode = "UPI",
                paymentType = "Extra Expense",
                remarks = "Purchased site alignment string and tape"
            ))
        }
    }
}
