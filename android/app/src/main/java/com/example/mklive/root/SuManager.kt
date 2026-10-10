package com.example.mklive.root

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Manages Magisk / KernelSU root permissions and privileged shell execution.
 *
 * Calling [requestRoot] invokes `su -c id`, which triggers Magisk's
 * interactive Superuser grant prompt on the device screen.
 */
object SuManager {

    private const val TAG = "SuManager"

    private val _isRootGranted = MutableStateFlow(false)
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()

    private val _rootInfo = MutableStateFlow<String?>("Not requested")
    val rootInfo: StateFlow<String?> = _rootInfo.asStateFlow()

    /**
     * Executes `su -c id` to prompt Magisk for Superuser permissions.
     * Must be run on a background thread.
     */
    suspend fun requestRoot(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Requesting root privileges from Magisk via 'su'...")
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            val exitCode = process.waitFor()

            val granted = exitCode == 0 && (line?.contains("uid=0") == true)
            _isRootGranted.value = granted
            _rootInfo.value = if (granted) line else "Denied (code $exitCode)"

            Log.i(TAG, "Root check result: exitCode=$exitCode, output=$line, granted=$granted")
            granted
        } catch (e: Exception) {
            Log.e(TAG, "Root request failed: ${e.message}", e)
            _isRootGranted.value = false
            _rootInfo.value = "Error: ${e.message}"
            false
        }
    }

    private val _isStandaloneRunning = MutableStateFlow(false)
    val isStandaloneRunning: StateFlow<Boolean> = _isStandaloneRunning.asStateFlow()

    /**
     * Checks if the local openkart/drive_web daemon is running on the device.
     */
    suspend fun checkStandaloneStatus(): Boolean = withContext(Dispatchers.IO) {
        val result = executeAsRoot("/data/local/mklive-standalone/status_standalone.sh")
        val isRunning = result.getOrNull()?.contains("RUNNING") == true
        Log.d(TAG, "checkStandaloneStatus: result=${result.getOrNull()} isRunning=$isRunning")
        _isStandaloneRunning.value = isRunning
        isRunning
    }

    /**
     * Starts the local standalone backend on the phone.
     */
    suspend fun startStandaloneBackend(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting standalone daemon on phone...")
        val res = executeAsRoot("/data/local/mklive-standalone/start_standalone.sh")
        Log.i(TAG, "Start script output: ${res.getOrNull()} error: ${res.exceptionOrNull()?.message}")
        kotlinx.coroutines.delay(1200)
        checkStandaloneStatus()
    }

    /**
     * Stops the local standalone backend on the phone.
     */
    suspend fun stopStandaloneBackend(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "Stopping standalone daemon on phone...")
        val res = executeAsRoot("/data/local/mklive-standalone/stop_standalone.sh")
        Log.i(TAG, "Stop script output: ${res.getOrNull()} error: ${res.exceptionOrNull()?.message}")
        kotlinx.coroutines.delay(500)
        checkStandaloneStatus()
    }

    /**
     * Executes an arbitrary command as root.
     */
    suspend fun executeAsRoot(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Prefer -mm (global mount namespace) so Termux binaries and network mounts are universally accessible
            var process = Runtime.getRuntime().exec(arrayOf("su", "-mm", "-c", command))
            var exitCode = process.waitFor()
            var reader = BufferedReader(InputStreamReader(process.inputStream))
            var output = reader.readText()
            var error = BufferedReader(InputStreamReader(process.errorStream)).readText()

            if (exitCode != 0 && error.contains("invalid option")) {
                // Fallback to standard su without -mm
                process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                exitCode = process.waitFor()
                output = BufferedReader(InputStreamReader(process.inputStream)).readText()
                error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            }

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(RuntimeException("Root command error ($exitCode): $error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
