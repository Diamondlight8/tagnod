package com.example.tagnod.nfc

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.widget.Toast
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object NfcWriteManager {

    var pendingTagId: String? = null
    var onTagWrittenCallback: (() -> Unit)? = null

    private val _writeResultEvents = MutableSharedFlow<NfcWriteResult>(extraBufferCapacity = 1)
    val writeResultEvents: SharedFlow<NfcWriteResult> = _writeResultEvents.asSharedFlow()

    fun startWriteSession(tagId: String, onSuccess: () -> Unit) {
        pendingTagId = tagId
        onTagWrittenCallback = onSuccess
    }

    fun stopWriteSession() {
        pendingTagId = null
        onTagWrittenCallback = null
    }

    fun handleIntent(context: Context, intent: Intent): Boolean {
        val targetTagId = pendingTagId ?: return false
        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_TAG_DISCOVERED == action
        ) {
            @Suppress("DEPRECATION")
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                val result = NfcWriteHelper.writeTagId(context, tag, targetTagId)
                _writeResultEvents.tryEmit(result)

                when (result) {
                    NfcWriteResult.SUCCESS -> {
                        Toast.makeText(context, "NFC tag registered successfully!", Toast.LENGTH_SHORT).show()
                        onTagWrittenCallback?.invoke()
                        stopWriteSession()
                    }
                    NfcWriteResult.READ_ONLY -> {
                        Toast.makeText(context, "Failed: NFC Tag is read-only", Toast.LENGTH_LONG).show()
                    }
                    NfcWriteResult.INSUFFICIENT_CAPACITY -> {
                        Toast.makeText(context, "Failed: NFC Tag capacity is too small", Toast.LENGTH_LONG).show()
                    }
                    NfcWriteResult.NOT_SUPPORTED -> {
                        Toast.makeText(context, "Failed: Tag does not support NDEF format", Toast.LENGTH_LONG).show()
                    }
                    NfcWriteResult.IO_ERROR -> {
                        Toast.makeText(context, "Failed: Connection timeout or write error", Toast.LENGTH_LONG).show()
                    }
                }
                return true
            }
        }
        return false
    }
}
