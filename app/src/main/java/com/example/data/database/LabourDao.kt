package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.models.Attendance
import com.example.data.models.AttendanceDraft
import com.example.data.models.AttendanceAuditLog
import com.example.data.models.SiteExpense
import com.example.data.models.RecentActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabourDao {
    // Attendance
    @Query("SELECT * FROM attendance WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getAllAttendance(userId: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Query("SELECT * FROM attendance WHERE userId = :userId AND date = :date")
    suspend fun getAttendanceByDate(userId: String, date: String): List<Attendance>

    // Attendance Audit Logs
    @Query("SELECT * FROM attendance_audit_logs WHERE userId = :userId ORDER BY editedAt DESC")
    fun getAllAttendanceAuditLogs(userId: String): Flow<List<AttendanceAuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceAuditLog(log: AttendanceAuditLog): Long

    // Attendance Drafts
    @Query("SELECT * FROM attendance_drafts WHERE userId = :userId AND date = :date")
    suspend fun getAttendanceDraftsByDate(userId: String, date: String): List<AttendanceDraft>

    @Query("SELECT * FROM attendance_drafts WHERE userId = :userId AND date = :date")
    fun getAttendanceDraftsByDateFlow(userId: String, date: String): Flow<List<AttendanceDraft>>

    @Query("SELECT * FROM attendance_drafts WHERE userId = :userId ORDER BY date DESC")
    fun getAllAttendanceDraftsFlow(userId: String): Flow<List<AttendanceDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceDraft(draft: AttendanceDraft): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceDrafts(drafts: List<AttendanceDraft>)

    @Update
    suspend fun updateAttendanceDraft(draft: AttendanceDraft)

    @Query("DELETE FROM attendance_drafts WHERE userId = :userId AND date = :date")
    suspend fun deleteAttendanceDraftsByDate(userId: String, date: String)


    // User Profiles
    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserProfile(uid: String): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // Sites
    @Query("SELECT * FROM sites WHERE userId = :userId ORDER BY id DESC")
    fun getAllSites(userId: String): Flow<List<Site>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: Site): Long

    @Update
    suspend fun updateSite(site: Site)

    @Query("DELETE FROM sites WHERE id = :id AND userId = :userId")
    suspend fun deleteSiteById(id: Int, userId: String)

    // Labour
    @Query("SELECT * FROM labour WHERE userId = :userId ORDER BY name ASC")
    fun getAllLabour(userId: String): Flow<List<Labour>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabour(labour: Labour): Long

    @Update
    suspend fun updateLabour(labour: Labour)

    @Query("DELETE FROM labour WHERE id = :id AND userId = :userId")
    suspend fun deleteLabourById(id: Int, userId: String)

    @Query("SELECT * FROM labour WHERE siteId = :siteId AND userId = :userId")
    fun getLabourBySite(siteId: Int, userId: String): Flow<List<Labour>>

    // Payments
    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY date DESC, id DESC")
    fun getAllPayments(userId: String): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Delete
    suspend fun deletePayment(payment: Payment)

    // Site Expenses
    @Query("SELECT * FROM site_expenses WHERE userId = :userId ORDER BY expenseDate DESC, id DESC")
    fun getAllSiteExpenses(userId: String): Flow<List<SiteExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSiteExpense(expense: SiteExpense): Long

    @Update
    suspend fun updateSiteExpense(expense: SiteExpense)

    @Query("DELETE FROM site_expenses WHERE id = :id AND userId = :userId")
    suspend fun deleteSiteExpenseById(id: Int, userId: String)

    // Recent Activities
    @Query("SELECT * FROM recent_activities WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllRecentActivities(userId: String): Flow<List<RecentActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentActivity(activity: RecentActivity): Long
}
