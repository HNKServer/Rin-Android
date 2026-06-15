package one.ethanthesleepy.androidew

import android.widget.Toast
import android.provider.Settings
import android.os.Environment
import android.os.Build
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : AppCompatActivity() {
    private var pendingDirectoryTarget: String? = null
    private val openDirectoryLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val path = Utilities.uriToPath(uri) ?: uri.toString()
            when (pendingDirectoryTarget) {
                "asset" -> findViewById<TextView>(R.id.asset_path)?.text = path
                "masterdata" -> findViewById<TextView>(R.id.masterdata_path)?.text = path
            }
        }
        pendingDirectoryTarget = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Instance = this

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (settingsPageVisible) {
                    closeSettings()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        
        init()
        if (!BackgroundService.isRunning() && Utilities.getSettings(this).launchOnStartup) {
            startServer()
        }

        try {
            Utilities.clearCache(this)
        } catch(_: Exception) {}
    }

    private var settingsPageVisible = false

    private fun applyWindowInsets(view: View) {
        val basePaddingLeft = view.paddingLeft
        val basePaddingTop = view.paddingTop
        val basePaddingRight = view.paddingRight
        val basePaddingBottom = view.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left + basePaddingLeft,
                systemBars.top + basePaddingTop,
                systemBars.right + basePaddingRight,
                systemBars.bottom + basePaddingBottom
            )
            insets
        }
        view.requestApplyInsets()
    }

    private fun init() {
        setContentView(R.layout.activity_main)

        applyWindowInsets(findViewById(R.id.main))
        
        setDownloadLinkText()
        setVersionTexts()

        //Log.d("ew", BackgroundService.isRunning().toString())
        if (BackgroundService.isRunning()) {
            setButtonText(getString(R.string.stop_server))
            setStatusText(getString(R.string.server_started))
            settingsPageVisible = false
        } else {
            setButtonText(getString(R.string.start_server))
            setStatusText(getString(R.string.server_stopped))
            settingsPageVisible = false
        }
    }

    private fun setVersionTexts() {
        val versionText = findViewById<TextView>(R.id.textView12)
        versionText.text = getString(R.string.app_version, BuildConfig.APP_VERSION_NUM)
        
        val internalVersionText = findViewById<TextView>(R.id.textView13)
        internalVersionText.text = getString(R.string.server_internal_version, BuildConfig.SERVER_VERSION_NUM)
    }


    private fun selectedExternalCdnPath(): String {
        val settings = Utilities.getSettings(this)
        val defaultRoot = (Utilities.getExternalDataPath(this) ?: "") + "/ew_data/"
        return settings.assetPath.ifBlank { defaultRoot + "assets" }
    }

    private fun needsAllFilesAccess(path: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
        if (Environment.isExternalStorageManager()) return false
        val appExternal = Utilities.getExternalDataPath(this) ?: ""
        val normalized = path.replace('\\', '/')
        // AndroidEw can always read its own app-specific external directory by raw path.
        if (appExternal.isNotBlank() && normalized.startsWith(appExternal)) return false
        return normalized.startsWith("/storage/emulated/0/") || normalized.startsWith("/sdcard/")
    }

    private fun requestAllFilesAccessForExternalCdn(path: String) {
        val message = getString(R.string.external_cdn_all_files_access_needed, path)
        setStatusText(message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
                intent.setData(Uri.parse("package:$packageName"))
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                try {
                    startActivity(Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"))
                } catch (_: ActivityNotFoundException) {
                    startActivity(Intent("android.settings.SETTINGS"))
                }
            }
        }
    }

    private fun startServer() {
        if (BackgroundService.isRunning()) return
        val assetPath = selectedExternalCdnPath()
        if (needsAllFilesAccess(assetPath)) {
            requestAllFilesAccessForExternalCdn(assetPath)
            return
        }
        setStatusText(getString(R.string.starting))
        val intent = Intent(this, AppBroadcastReceiver::class.java)
        intent.action = "START_SERVICE"
        sendBroadcast(intent)
    }

    private fun stopServer() {
        if (!BackgroundService.isRunning()) return
        setStatusText(getString(R.string.stopping))
        val intent = Intent(this, AppBroadcastReceiver::class.java)
        intent.action = "STOP_SERVICE"
        sendBroadcast(intent)
    }

    fun toggleServer(view: View?) {
        if (BackgroundService.isRunning()) {
            stopServer()
            setButtonText(getString(R.string.start_server))
            setStatusText(getString(R.string.server_stopped))
        } else {
            startServer()
            setButtonText(getString(R.string.stop_server))
            setStatusText(getString(R.string.server_started))
        }
    }

    private fun setDownloadLinkText() {
        val status = findViewById<View>(R.id.downloadLinks) as TextView
        status.text = Html.fromHtml(
            getString(R.string.apk_folder_notice), Html.FROM_HTML_MODE_COMPACT)

        status.movementMethod = LinkMovementMethod.getInstance()
    }

    fun setStatusText(value: String) {
        if (findViewById<View>(R.id.status) == null) {
            return
        }
        val status = findViewById<View>(R.id.status) as TextView
        status.text = value
    }
    fun setButtonText(value: String) {
        if (findViewById<View>(R.id.restart_server) == null) {
            return
        }
        val restart = findViewById<Button>(R.id.restart_server)
        restart.text = value
    }

    // Begin Settings

    private fun loadSettings() {
        val settings = Utilities.getSettings(this)
        val defaultRoot = (Utilities.getExternalDataPath(this) ?: "") + "/ew_data/"
        findViewById<CheckBox>(R.id.startup).isChecked = settings.launchOnStartup
        findViewById<CheckBox>(R.id.easter).isChecked = settings.easterMode
        findViewById<TextView>(R.id.asset_path).text = settings.assetPath.ifBlank { defaultRoot + "assets" }
        findViewById<TextView>(R.id.masterdata_path).text = settings.masterdataPath.ifBlank { defaultRoot + "masterdata" }
    }

    private fun saveSettings() {
        val settings = Settings(
            launchOnStartup = findViewById<CheckBox>(R.id.startup).isChecked,
            easterMode = findViewById<CheckBox>(R.id.easter).isChecked,
            assetPath = findViewById<TextView>(R.id.asset_path).text.toString(),
            masterdataPath = findViewById<TextView>(R.id.masterdata_path).text.toString()
        )
        BackgroundService.Instance?.setEasterMode(settings.easterMode)
        val json = Json.encodeToString(settings)
        File(filesDir.absolutePath + "/settings.json").writeText(json)
        if (needsAllFilesAccess(settings.assetPath)) {
            requestAllFilesAccessForExternalCdn(settings.assetPath)
        }
    }

    private fun setupSettingsMenu() {
        loadSettings()
        val git = findViewById<View>(R.id.git) as TextView
        git.text = Html.fromHtml(
            getString(R.string.git_notice), Html.FROM_HTML_MODE_COMPACT)
        git.movementMethod = LinkMovementMethod.getInstance()
    }

    fun openSettings(view: View?) {
        setContentView(R.layout.settings)
        applyWindowInsets(findViewById(R.id.settings_root))

        setupSettingsMenu()
        settingsPageVisible = true
    }
    
    fun closeSettings(view: View? = null) {
        saveSettings()
        settingsPageVisible = false
        init()
    }

    fun chooseAssetPath(view: View?) {
        pendingDirectoryTarget = "asset"
        openDirectoryLauncher.launch(null)
    }

    fun chooseMasterdataPath(view: View?) {
        pendingDirectoryTarget = "masterdata"
        openDirectoryLauncher.launch(null)
    }

    fun checkForUpdates(view: View?) {
        val currentVersion = Utilities.getCurrentVersion(this)
        if (currentVersion == null) {
            Utilities.sendNotification(this, "ew_updates", "Updates", "Application updates", "Failed to get installed version!")
            return
        }
        Utilities.getLatestVersion { latestVersion ->
            if (latestVersion == null) {
                Utilities.sendNotification(this, "ew_updates", "Updates", "Application updates", "Failed to fetch latest version!")
                return@getLatestVersion
            }
            //Log.d("ew-update", currentVersion)
            //Log.d("ew-update", latestVersion)
            if (currentVersion == latestVersion) {
                Utilities.sendNotification(this, "ew_updates", "Updates", "Application updates", "App up to date!")
                return@getLatestVersion
            }
            Utilities.downloadFileWithProgress(this,
                "https://git.ethanthesleepy.one/ethanaobrien/ew-android/releases/download/latest/app-release.apk",
                filesDir.absolutePath + "/temp/app-release.apk",
                "downloading_update",
                "Downloading update",
                onDownloadComplete = { downloadedFilePath ->
                    Utilities.installFile(this, downloadedFilePath)
                }
            )
        }
    }

    @Serializable
    data class Settings(
        val launchOnStartup: Boolean,
        val easterMode: Boolean,
        val assetPath: String = "",
        val masterdataPath: String = ""
    )

    // End Settings

    companion object {
        @JvmStatic
        var Instance: MainActivity? = null
    }
}