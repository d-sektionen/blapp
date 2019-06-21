package se.dsektionen.blapp

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import android.provider.Settings.ACTION_NFC_SETTINGS


class MainActivity : AppCompatActivity(), Nfc.NfcListener {

    var nfc: Nfc? = null
    private var hex: Boolean = false
    private var reverse: Boolean = false
    private var uidEnable: Boolean = false
    private var enter: Boolean = false
    private var url: String = ""


    // Converts a byte array uid to something usable, depending on preferences.
    private fun byteArrayToString(arr: ByteArray): String {
        var ubyteArray = arr.asUByteArray()

        if (reverse) ubyteArray = ubyteArray.reversedArray()

        val uint: UInt = ubyteArray.fold(0.toUInt()) { acc: UInt, byte -> (acc shl 8) + byte.toUInt() }

        val rv = uint.toString(if (hex) 16 else 10)

        return if (enter) rv + '\n' else rv
    }


    // Show messages when nfc is toggled.
    override fun nfcUpdated(quiet: Boolean) {
        val nfcEnabled = nfc!!.isEnabled()
        val nfcSupported = nfc!!.isSupported()

        if (!nfcSupported)
            Snackbar
                .make(webView, "NFC is not supported on this device", Snackbar.LENGTH_INDEFINITE)
                .show()

        else if (nfcEnabled && !quiet)
            Snackbar
                .make(webView, "NFC is now enabled", Snackbar.LENGTH_SHORT)
                .show()
        else if (!nfcEnabled)
            Snackbar
                .make(webView, "NFC is disabled", Snackbar.LENGTH_INDEFINITE)
                .setAction("Enable NFC") {
                    startActivity(Intent(ACTION_NFC_SETTINGS))
                }
                .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // read previously set settings
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        url = sharedPreferences.getString("url", resources.getString(R.string.url_default))
        uidEnable = sharedPreferences.getBoolean("uid_enable", resources.getBoolean(R.bool.uid_enable_default))
        hex = sharedPreferences.getBoolean("uid_hex", resources.getBoolean(R.bool.uid_hex_default))
        reverse = sharedPreferences.getBoolean("uid_reverse", resources.getBoolean(R.bool.uid_reverse_default))
        enter = sharedPreferences.getBoolean("uid_enter", resources.getBoolean(R.bool.uid_enter_default))

        // init nfc
        nfc = Nfc(this, this)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // init web view
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    private fun dispatchKeyEvents(keys: List<Int>) {
        // Recursively fires the keyboard events to show the characters on screen and
        // give the user a feel for what the app is actually doing.

        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keys[0]))
        webView.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keys[0]))

        if (keys.size > 1) {
            Handler().postDelayed({
                dispatchKeyEvents(keys.subList(1, keys.size))
            }, 10)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {

            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            if (uidEnable) {
                val uid = byteArrayToString(tag.id)
                dispatchKeyEvents(KeyCodeUtil.stringToKeyCodeList(uid))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // fake an nfc toggle in case user has nfc disabled when resuming the app
        nfcUpdated(true)

        nfc!!.enableForeground()
    }
    override fun onPause() {
        super.onPause()

        nfc!!.disableForeground()
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.action_refresh -> webView.loadUrl(url)
        }
        return super.onOptionsItemSelected(item)
    }
}
