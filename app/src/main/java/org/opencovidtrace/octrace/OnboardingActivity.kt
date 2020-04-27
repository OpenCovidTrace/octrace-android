package org.opencovidtrace.octrace

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.opencovidtrace.octrace.MainActivity.Companion.REQUEST_LOCATION
import org.opencovidtrace.octrace.OnboardingActivity.Extra.STAGE_EXTRA
import org.opencovidtrace.octrace.storage.KeyManager
import org.opencovidtrace.octrace.utils.SecurityUtil

class OnboardingActivity : AppCompatActivity() {

    object Extra {
        const val STAGE_EXTRA = "STAGE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val textView: TextView = findViewById(R.id.text)
        val titleView: TextView = findViewById(R.id.title)
        val button: Button = findViewById(R.id.button)

        val stage = intent.getSerializableExtra(STAGE_EXTRA) as OnboardingStage

        when (stage) {
            OnboardingStage.WELCOME -> {
                titleView.setText(R.string.onboarding_welcome_title)
                textView.setText(R.string.onboarding_welcome_text)
                button.setText(R.string.onboarding_welcome_button)
            }
            OnboardingStage.LOCATION -> {
                titleView.setText(R.string.onboarding_location_title)
                textView.setText(R.string.onboarding_location_text)
                button.setText(R.string.onboarding_location_button)
            }
        }

        button.setOnClickListener {
            when (stage) {
                OnboardingStage.WELCOME -> {
                    KeyManager.setKey(SecurityUtil.generateKey())

                    goNext(OnboardingStage.LOCATION)
                }
                OnboardingStage.LOCATION -> ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION
                )
            }
        }
    }

    override fun onBackPressed() {
        // We do not allow back navigation for onboarding!
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_LOCATION -> {
                finishOk()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        passThroughIfOk(resultCode)

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun goNext(nextStage: OnboardingStage) {
        val intent = Intent(this, OnboardingActivity::class.java)
        intent.putExtra(STAGE_EXTRA, nextStage)
        startActivityForResult(intent, 0)
    }

    private fun passThroughIfOk(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            finishOk()
        }
    }

    private fun finishOk() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}

internal enum class OnboardingStage {
    WELCOME, LOCATION
}
