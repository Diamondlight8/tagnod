package com.example.tagnod.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.util.Log

enum class NfcWriteResult {
    SUCCESS,
    READ_ONLY,
    INSUFFICIENT_CAPACITY,
    NOT_SUPPORTED,
    IO_ERROR
}

object NfcWriteHelper {

    private const val TAG = "NfcWriteHelper"

    fun enableForegroundDispatch(activity: Activity) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)

        val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            addDataScheme("tagnod")
        }
        val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
        val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)

        val filters = arrayOf(ndefFilter, techFilter, tagFilter)
        nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        nfcAdapter.disableForegroundDispatch(activity)
    }

    fun writeTagId(context: Context, tag: Tag, uniqueTagId: String): NfcWriteResult {
        val uri = Uri.parse("tagnod://tag?id=$uniqueTagId")
        val uriRecord = NdefRecord.createUri(uri)
        val aarRecord = NdefRecord.createApplicationRecord(context.packageName)
        val ndefMessage = NdefMessage(arrayOf(uriRecord, aarRecord))

        val ndef = Ndef.get(tag)
        return if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    Log.e(TAG, "Tag is read-only")
                    NfcWriteResult.READ_ONLY
                } else if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    ndef.close()
                    Log.e(TAG, "Tag capacity is too small")
                    NfcWriteResult.INSUFFICIENT_CAPACITY
                } else {
                    ndef.writeNdefMessage(ndefMessage)
                    ndef.close()
                    Log.d(TAG, "Successfully wrote NDEF URI and AAR to tag: $uniqueTagId")
                    NfcWriteResult.SUCCESS
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing NDEF tag", e)
                try { ndef.close() } catch (_: Exception) {}
                NfcWriteResult.IO_ERROR
            }
        } else {
            val formatable = NdefFormatable.get(tag)
            if (formatable != null) {
                try {
                    formatable.connect()
                    formatable.format(ndefMessage)
                    formatable.close()
                    Log.d(TAG, "Successfully formatted and wrote NDEF URI and AAR to tag: $uniqueTagId")
                    NfcWriteResult.SUCCESS
                } catch (e: Exception) {
                    Log.e(TAG, "Error formatting NDEF tag", e)
                    try { formatable.close() } catch (_: Exception) {}
                    NfcWriteResult.IO_ERROR
                }
            } else {
                Log.e(TAG, "Tag does not support NDEF or NdefFormatable")
                NfcWriteResult.NOT_SUPPORTED
            }
        }
    }
}
