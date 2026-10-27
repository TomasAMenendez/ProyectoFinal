package com.proyectoteo.expo

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var juego: WebView
    private lateinit var servidor: ServidorLocal
    private var pedidoCamara: PermissionRequest? = null

    private val permisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedida ->
        val pedido = pedidoCamara
        pedidoCamara = null
        if (pedido == null) return@registerForActivityResult
        if (concedida) pedido.grant(pedido.resources) else pedido.deny()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        setContentView(R.layout.activity_main)
        servidor = ServidorLocal(assets)
        juego = findViewById(R.id.juego)
        juego.setBackgroundColor(getColor(R.color.cielo))
        juego.settings.javaScriptEnabled = true
        juego.settings.domStorageEnabled = true
        juego.settings.mediaPlaybackRequiresUserGesture = false
        juego.settings.allowFileAccess = false
        juego.settings.allowContentAccess = false
        juego.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host
                return host != "127.0.0.1" && host != "localhost"
            }
        }
        juego.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                val host = request.origin.host
                val local = host == "127.0.0.1" || host == "localhost"
                val soloVideo = request.resources.isNotEmpty() &&
                    request.resources.all { it == PermissionRequest.RESOURCE_VIDEO_CAPTURE }
                if (!local || !soloVideo) {
                    request.deny()
                    return
                }
                val ya = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
                if (ya) {
                    request.grant(request.resources)
                    return
                }
                pedidoCamara?.deny()
                pedidoCamara = request
                permisoCamara.launch(Manifest.permission.CAMERA)
            }
        }
        juego.loadUrl("http://127.0.0.1:${servidor.puerto}/index.html")
    }

    override fun onDestroy() {
        pedidoCamara?.deny()
        pedidoCamara = null
        if (this::juego.isInitialized) juego.destroy()
        if (this::servidor.isInitialized) servidor.cerrar()
        super.onDestroy()
    }
}
