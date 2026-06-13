package one.ethanthesleepy.androidew

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File


class StartupActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (File(filesDir.absolutePath + "/initDone").exists()) {
            val main = Intent(
                this,
                MainActivity::class.java
            )
            startActivity(main)
            finish()
            return
        }
        enableEdgeToEdge()
        setContentView(R.layout.startup_main)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun switchToLayout(view: View?, layout: Int) {
        val root = findViewById<ViewGroup>(android.R.id.content)
        
        val transition = TransitionSet().apply {
            addTransition(Fade().apply { duration = 300 })
            addTransition(Slide(Gravity.END).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
            })
            ordering = TransitionSet.ORDERING_TOGETHER
        }

        TransitionManager.beginDelayedTransition(root, transition)

        setContentView(layout)

        when (layout) {
            R.layout.install_game -> setupInstallGameLayout()
        }
    }

    private fun setupInstallGameLayout() {
        findViewById<TextView>(R.id.downloadManual)?.apply {
            text = Html.fromHtml(getString(R.string.download_manual), Html.FROM_HTML_MODE_COMPACT)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    fun startSetup(view: View?) {
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            switchToLayout(view, R.layout.install_game)
        } else {
            switchToLayout(view, R.layout.permission_request)
        }
    }

    fun requestNotificationPermissions(view: View) {
        val status = findViewById<TextView>(R.id.permissionStatus)
        val button = view as Button
        
        if (button.text == getString(R.string.continue_txt)) {
            switchToLayout(view, R.layout.install_game)
            return
        }
        
        if (Build.VERSION.SDK_INT >= 33) {
            pushNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            status?.text = getString(R.string.unable_to_request_permission)
            button.text = getString(R.string.continue_txt)
        }
    }

    private val pushNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val status = findViewById<TextView>(R.id.permissionStatus)
        val button = findViewById<Button>(R.id.requestPermission)
        
        if (granted) {
            status?.text = getString(R.string.permission_granted)
        } else {
            status?.text = getString(R.string.notification_permission_denied)
        }
        button?.text = getString(R.string.continue_txt)
    }

    fun downloadJP(view: View) {
        startDownload("https://ethanthesleepy.one/public/lovelive/sif2/localhost/sif2-jp.apk", "sif2-jp.apk")
    }

    fun downloadGL(view: View) {
        startDownload("https://ethanthesleepy.one/public/lovelive/sif2/localhost/sif2-gl.zip", "sif2-gl.zip")
    }

    private fun startDownload(url: String, fileName: String) {
        val statusText = findViewById<TextView>(R.id.downloadStatus)
        val jpBtn = findViewById<Button>(R.id.downloadJpBtn)
        val glBtn = findViewById<Button>(R.id.downloadGlBtn)
        val continueBtn = findViewById<Button>(R.id.button2)
        val destinationPath = "${filesDir.absolutePath}/temp/$fileName"
        
        statusText?.apply {
            text = getString(R.string.download_starting)
            movementMethod = LinkMovementMethod.getInstance()
        }
        
        jpBtn?.visibility = View.GONE
        glBtn?.visibility = View.GONE
        continueBtn?.visibility = View.GONE

        Utilities.downloadFileWithProgress(
            context = this,
            fileUrl = url,
            destinationPath = destinationPath,
            channelId = "setup_download",
            channelName = "Setup Downloads",
            showNotification = false,
            onProgress = { progress ->
                runOnUiThread {
                    statusText?.text = if (progress >= 0) {
                        getString(R.string.download_progress, progress)
                    } else {
                        getString(R.string.downloading_generic)
                    }
                }
            },
            onDownloadComplete = { path ->
                runOnUiThread {
                    statusText?.text = getString(R.string.download_complete_installing)
                    Utilities.installFile(this, path)
                    continueBtn?.visibility = View.VISIBLE
                }
            },
            onDownloadError = { _ ->
                runOnUiThread {
                    statusText?.text = Html.fromHtml(
                        getString(R.string.download_failed_manual),
                        Html.FROM_HTML_MODE_COMPACT
                    )
                    jpBtn?.visibility = View.VISIBLE
                    glBtn?.visibility = View.VISIBLE
                    continueBtn?.visibility = View.VISIBLE
                }
            }
        )
    }

    fun showSetupDone(view: View?) {
        switchToLayout(view, R.layout.setup_done)
    }

    fun finishSetup(view: View?) {
        File(filesDir.absolutePath + "/initDone").writeText("true")
        val main = Intent(this, MainActivity::class.java)
        startActivity(main)
        finish()
    }
}
