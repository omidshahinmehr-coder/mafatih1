package com.mafatih.reader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val toolbar = findViewById<Toolbar>(R.id.aboutToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val versionText = findViewById<android.widget.TextView>(R.id.aboutVersion)
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
        versionText.text = "${getString(R.string.about_version_prefix)} $versionName"

        val emailView = findViewById<android.widget.TextView>(R.id.aboutEmail)
        emailView.setOnClickListener { sendEmail() }
    }

    private fun sendEmail() {
        val email = getString(R.string.about_email_value)
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, email, Toast.LENGTH_LONG).show()
        }
    }
}
