package se.dsektionen.blapp

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.NfcManager

class Nfc(
    private var activity: Activity,
    private var nfcListener: NfcListener
) {
    private var nfcManager = activity.getSystemService(Context.NFC_SERVICE) as NfcManager
    private var nfcAdapter = nfcManager.defaultAdapter

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val action = intent.action

            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                when (intent.getIntExtra(
                    NfcAdapter.EXTRA_ADAPTER_STATE,
                    NfcAdapter.STATE_OFF
                )) {
                    NfcAdapter.STATE_OFF -> nfcListener.nfcUpdated()
                    NfcAdapter.STATE_ON -> nfcListener.nfcUpdated()
                }
            }
        }
    }

    fun isSupported() : Boolean {
        return nfcAdapter != null
    }

    fun isEnabled() : Boolean {
        return isSupported() && nfcAdapter.isEnabled
    }

    fun enableForeground() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity.registerReceiver(broadcastReceiver, filter)

        val intent = PendingIntent.getActivity(
            activity, 0, Intent(
                activity,
                activity.javaClass
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )

        if (isSupported()) {
            nfcAdapter.enableForegroundDispatch(activity, intent, null, null)
        }
    }

    fun disableForeground() {
        activity.unregisterReceiver(broadcastReceiver)
        if (isSupported()) {
            nfcAdapter.disableForegroundDispatch(activity)
        }
    }

    interface NfcListener {
        fun nfcUpdated(quiet: Boolean = false)
    }
}