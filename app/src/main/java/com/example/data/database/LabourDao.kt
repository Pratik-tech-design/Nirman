package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.models.Attendance
import com.example.data.models.AttendanceDraft
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
}
