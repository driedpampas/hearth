/*
 * Hearth: An offline AI chat application for Android.
 * Copyright (C) 2026 syulze <me@syu.nl.eu.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package org.eu.nl.syu.hearth.common.crash

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Global handler for uncaught exceptions.
 * It captures the stack trace and launches [CrashReportActivity] in a separate process.
 */
class GlobalCrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            val intent = Intent(context, CrashReportActivity::class.java).apply {
                putExtra(CrashReportActivity.EXTRA_CRASH_INFO, stackTrace)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("GlobalCrashHandler", "Error handling uncaught exception", e)
        } finally {
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"

        /**
         * Initializes the global crash handler.
         * Should be called from [android.app.Application.onCreate].
         */
        fun initialize(context: Context) {
            if (isCrashReporterProcess()) return
            Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(context))
        }

        private fun isCrashReporterProcess(): Boolean {
            val processName = android.app.Application.getProcessName()
            return processName?.endsWith(":crash_handler") == true
        }

        private fun getProcessNameLegacy(context: Context): String? {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = am.runningAppProcesses ?: return null
            val pid = Process.myPid()
            return processes.find { it.pid == pid }?.processName
        }

        /**
         * Checks for crashes or ANRs from the previous session.
         */
        fun checkPostMortemCrash(context: Context): String? {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val exitReasons = am.getHistoricalProcessExitReasons(null, 0, 1)
            if (exitReasons.isNotEmpty()) {
                val exitInfo = exitReasons[0]
                val reason = exitInfo.reason

                if (reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    reason == android.app.ApplicationExitInfo.REASON_ANR ||
                    reason == android.app.ApplicationExitInfo.REASON_EXIT_SELF) {

                    // For REASON_EXIT_SELF, we only care if it was a crash-related exit
                    // REASON_EXIT_SELF is often what we get when we call killProcess() in our handler.

                    val trace = exitInfo.traceInputStream?.bufferedReader()?.use { it.readText() }
                    if (!trace.isNullOrBlank()) {
                        val reasonStr = when(reason) {
                            android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native Crash"
                            android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
                            android.app.ApplicationExitInfo.REASON_EXIT_SELF -> "Previous Session Crash"
                            else -> "Unexpected Exit"
                        }
                        return "Type: $reasonStr\nTimestamp: ${java.util.Date(exitInfo.timestamp)}\n\n$trace"
                    }
                }
            }
            return null
        }
    }
}
