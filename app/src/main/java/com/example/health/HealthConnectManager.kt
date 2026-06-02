package com.example.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

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
}
