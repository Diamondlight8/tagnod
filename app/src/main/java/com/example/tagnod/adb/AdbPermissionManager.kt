package com.example.tagnod.adb

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import android.util.Log
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket

object AdbPermissionManager {

    private const val TAG = "AdbPermissionManager"

    fun isPermissionGranted(context: Context): Boolean {
        return context.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED
    }

    private val base64Impl = object : AdbBase64 {
        override fun encodeToString(ba: ByteArray?): String {
            return Base64.encodeToString(ba, Base64.NO_WRAP)
        }
    }

    private fun getAdbCrypto(context: Context): AdbCrypto {
        val keyFile = File(context.filesDir, "adb_key")
        val pubKeyFile = File(context.filesDir, "adb_key.pub")

        return if (keyFile.exists() && pubKeyFile.exists()) {
            try {
                AdbCrypto.loadAdbKeyPair(base64Impl, keyFile, pubKeyFile)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load existing ADB key pair, generating new one", e)
                val newCrypto = AdbCrypto.generateAdbKeyPair(base64Impl)
                newCrypto.saveAdbKeyPair(keyFile, pubKeyFile)
                newCrypto
            }
        } else {
            val newCrypto = AdbCrypto.generateAdbKeyPair(base64Impl)
            newCrypto.saveAdbKeyPair(keyFile, pubKeyFile)
            newCrypto
        }
    }

    suspend fun grantPermissionViaAdb(
        context: Context,
        port: Int = 5555
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (isPermissionGranted(context)) {
            return@withContext Result.success(Unit)
        }

        var socket: Socket? = null
        var adbConnection: AdbConnection? = null

        try {
            Log.d(TAG, "Connecting to localhost ADB daemon on port $port...")
            socket = Socket("127.0.0.1", port)
            socket.tcpNoDelay = true

            val crypto = getAdbCrypto(context)
            adbConnection = AdbConnection.create(socket, crypto)
            adbConnection.connect()

            val command = "pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS\n"
            Log.d(TAG, "Executing command: $command")

            val stream = adbConnection.open("shell:$command")

            // Read response
            while (!stream.isClosed) {
                try {
                    val bytes = stream.read()
                    val output = String(bytes)
                    Log.d(TAG, "ADB output: $output")
                } catch (_: Exception) {
                    break
                }
            }

            try { stream.close() } catch (_: Exception) {}

            // Verify permission was granted
            if (isPermissionGranted(context)) {
                Log.d(TAG, "Successfully granted WRITE_SECURE_SETTINGS via Wireless ADB!")
                Result.success(Unit)
            } else {
                Result.failure(Exception("ADB connection succeeded, but permission grant command failed. Ensure Wireless Debugging is active."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Wireless ADB on localhost:$port", e)
            Result.failure(Exception("Could not connect to Wireless ADB on localhost:$port (${e.localizedMessage ?: "Connection refused"}). Make sure Wireless Debugging is enabled in Developer Options."))
        } finally {
            try { adbConnection?.close() } catch (_: Exception) {}
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
