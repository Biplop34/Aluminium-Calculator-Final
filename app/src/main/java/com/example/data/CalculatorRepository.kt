package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.Base8HistoryItem
import com.example.model.CalculationResult
import com.example.model.CustomerDetails
import com.example.model.MeasurementSet
import com.example.model.SavedJob
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class CalculatorRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("alum_calc_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val savedJobsType = Types.newParameterizedType(List::class.java, SavedJob::class.java)
    private val savedJobsAdapter = moshi.adapter<List<SavedJob>>(savedJobsType)

    private val base8HistoryType = Types.newParameterizedType(List::class.java, Base8HistoryItem::class.java)
    private val base8HistoryAdapter = moshi.adapter<List<Base8HistoryItem>>(base8HistoryType)

    private val customerAdapter = moshi.adapter(CustomerDetails::class.java)

    fun getThaiSize(): String {
        return prefs.getString("thai_size", "3") ?: "3"
    }

    fun setThaiSize(size: String) {
        prefs.edit().putString("thai_size", size).apply()
    }

    fun getCustomerDetails(): CustomerDetails {
        val json = prefs.getString("customer_details", null) ?: return CustomerDetails()
        return try {
            customerAdapter.fromJson(json) ?: CustomerDetails()
        } catch (e: Exception) {
            CustomerDetails()
        }
    }

    fun saveCustomerDetails(details: CustomerDetails) {
        val json = customerAdapter.toJson(details)
        prefs.edit().putString("customer_details", json).apply()
    }

    fun getSavedJobs(): List<SavedJob> {
        val json = prefs.getString("saved_jobs", null) ?: return emptyList()
        return try {
            savedJobsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveJob(job: SavedJob) {
        val current = getSavedJobs().toMutableList()
        current.removeAll { it.id == job.id }
        current.add(0, job)
        // Keep last 50 jobs
        val trimmed = current.take(50)
        prefs.edit().putString("saved_jobs", savedJobsAdapter.toJson(trimmed)).apply()
    }

    fun deleteJob(jobId: String) {
        val current = getSavedJobs().toMutableList()
        current.removeAll { it.id == jobId }
        prefs.edit().putString("saved_jobs", savedJobsAdapter.toJson(current)).apply()
    }

    fun clearAllJobs() {
        prefs.edit().remove("saved_jobs").apply()
    }

    fun getBase8History(): List<Base8HistoryItem> {
        val json = prefs.getString("base8_history", null) ?: return emptyList()
        return try {
            base8HistoryAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveBase8History(items: List<Base8HistoryItem>) {
        val trimmed = items.takeLast(30)
        prefs.edit().putString("base8_history", base8HistoryAdapter.toJson(trimmed)).apply()
    }

    fun clearBase8History() {
        prefs.edit().remove("base8_history").apply()
    }
}
