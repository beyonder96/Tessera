package com.example.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.permission.HealthPermission
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }
    
    val permissions = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getWritePermission(HeightRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    fun checkAvailability(): Int {
        return HealthConnectClient.getSdkStatus(context)
    }

    suspend fun readWeightRecords(start: Instant, end: Instant): List<WeightRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readHeightRecords(start: Instant, end: Instant): List<HeightRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readSleepRecords(start: Instant, end: Instant): List<SleepSessionRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun readStepsRecords(start: Instant, end: Instant): List<StepsRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun writeWeightRecord(weightKg: Double, timestamp: Long) {
        try {
            val record = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = Instant.ofEpochMilli(timestamp),
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
            )
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun writeSleepRecord(startTime: Long, endTime: Long) {
        try {
            val record = SleepSessionRecord(
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
            )
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun writeStepsRecord(count: Long, startTime: Long, endTime: Long) {
        try {
            val record = StepsRecord(
                count = count,
                startTime = Instant.ofEpochMilli(startTime),
                endTime = Instant.ofEpochMilli(endTime),
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())
            )
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
