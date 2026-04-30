package org.eu.nl.syu.charchat.common

import android.app.ActivityManager
import android.content.Context
import java.util.Locale

object ModelSizeUtils {
    /**
     * Parses the model size from a string name.
     * Examples:
     * - FastVLM-0.5B -> 0.5
     * - gemma-3-270m-it -> 0.27
     * - gemma-4-E2B-it -> 2.0
     * 
     * Returns parameters in billions.
     */
    fun parseModelSize(name: String): Double? {
        val lower = name.lowercase(Locale.US)
        
        // Handle special cases like E2B
        if (lower.contains("e2b")) return 2.0
        
        // Regex for B (billions)
        val bRegex = Regex("""(\d+(?:\.\d+)?)\s*b""")
        val bMatch = bRegex.find(lower)
        if (bMatch != null) {
            return bMatch.groupValues[1].toDoubleOrNull()
        }
        
        // Regex for M (millions)
        val mRegex = Regex("""(\d+(?:\.\d+)?)\s*m""")
        val mMatch = mRegex.find(lower)
        if (mMatch != null) {
            val millions = mMatch.groupValues[1].toDoubleOrNull() ?: return null
            return millions / 1000.0
        }
        
        return null
    }

    /**
     * Estimates RAM usage in bytes based on parameter count.
     * Assumes ~1.2 bytes per parameter (4-bit quantization + some overhead).
     */
    fun estimateMemoryUsage(paramsInBillions: Double): Long {
        val params = paramsInBillions * 1_000_000_000L
        return (params * 1.2).toLong()
    }

    enum class Compatibility {
        FITS,    // Fits comfortably
        CLOSE,   // Fits but might be tight
        TOO_BIG, // Likely to cause OOM or extreme lag
        UNKNOWN  // Size could not be inferred
    }

    /**
     * Determines if a model can fit on the current device.
     */
    fun checkCompatibility(context: Context, paramsInBillions: Double?): Compatibility {
        if (paramsInBillions == null) return Compatibility.UNKNOWN

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRam = memoryInfo.totalMem
        val estimatedUsage = estimateMemoryUsage(paramsInBillions)
        
        // Safety margin: 1.5GB or 15% of total RAM, whichever is higher.
        val safetyMargin = maxOf(1_500_000_000L, (totalRam * 0.15).toLong())
        val maxSafeUsage = totalRam - safetyMargin
        
        return when {
            estimatedUsage < maxSafeUsage * 0.85 -> Compatibility.FITS
            estimatedUsage < maxSafeUsage -> Compatibility.CLOSE
            else -> Compatibility.TOO_BIG
        }
    }
    
    fun formatParameterCount(paramsInBillions: Double): String {
        return if (paramsInBillions >= 1.0) {
            String.format(Locale.US, "%.1fB", paramsInBillions)
        } else {
            String.format(Locale.US, "%dM", (paramsInBillions * 1000).toInt())
        }
    }
}
