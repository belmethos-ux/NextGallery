package com.hakki.nextgallery

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hakki.nextgallery.api.NextcloudClient
import com.hakki.nextgallery.databinding.ActivityLoginBinding
import com.hakki.nextgallery.util.Prefs
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = Prefs(this)

        if (prefs.isLoggedIn) {
            goToGallery()
            return
        }

        binding.buttonConnect.setOnClickListener { attemptConnect() }
    }

    private fun attemptConnect() {
        val server = binding.editServerUrl.text?.toString()?.trim()?.trimEnd('/') ?: ""
        val user = binding.editUsername.text?.toString()?.trim() ?: ""
        val pass = binding.editPassword.text?.toString()?.trim() ?: ""
        val folder = binding.editFolder.text?.toString()?.trim() ?: ""

        if (server.isBlank() || user.isBlank() || pass.isBlank()) {
            showError("Server, username and app password are all required.")
            return
        }
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            showError("Server URL must start with https:// (or http:// for local testing).")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val client = NextcloudClient(server, user, pass)
            val ok = try {
                // A cheap way to validate credentials: list the root (or chosen folder) once.
                client.listPhotosRecursive(folder, maxDepth = 1)
                true
            } catch (e: Exception) {
                false
            }

            setLoading(false)
            if (ok) {
                prefs.serverUrl = server
                prefs.username = user
                prefs.appPassword = pass
                prefs.photosFolder = folder
                goToGallery()
            } else {
                showError("Couldn't connect. Check the server URL, username and app password.")
            }
        }
    }

    private fun goToGallery() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.loginProgress.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
        binding.buttonConnect.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.loginError.text = message
        binding.loginError.visibility = android.view.View.VISIBLE
    }
}
