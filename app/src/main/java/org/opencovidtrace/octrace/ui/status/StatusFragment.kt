package org.opencovidtrace.octrace.ui.status

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_status.*
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.ext.ui.choose
import org.opencovidtrace.octrace.ext.ui.confirm
import org.opencovidtrace.octrace.ext.ui.showInfo
import org.opencovidtrace.octrace.service.TrackingService
import org.opencovidtrace.octrace.storage.KeysManager
import org.opencovidtrace.octrace.storage.TracksManager
import org.opencovidtrace.octrace.storage.UserSettingsManager

class StatusFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changeStatusButton.setOnClickListener { changeStatus() }

        recordTrackSwitch.setOnClickListener {
            UserSettingsManager.recordTrack = recordTrackSwitch.isChecked

            activity?.startService(Intent(activity, TrackingService::class.java))
        }

        shareTrackSwitch.setOnClickListener {
            UserSettingsManager.uploadTrack = shareTrackSwitch.isChecked
        }

        shareMetaSwitch.setOnClickListener {
            UserSettingsManager.discloseMetaData = shareMetaSwitch.isChecked
        }

        refreshStatus()
    }

    override fun onResume() {
        super.onResume()

        // This value could've change through on-boarding so we have to force refresh
        recordTrackSwitch.isChecked = UserSettingsManager.recordTrack
    }

    private fun changeStatus() {
        if (UserSettingsManager.sick()) {
            showInfo(R.string.whats_next_info)
        } else {
            confirm(R.string.report_exposure_confirmation) {
                updateUserStatus(UserSettingsManager.EXPOSED)

                choose(
                    R.string.tracks_upload_confirmation,
                    {
                        UserSettingsManager.uploadTrack = true
                        shareTrackSwitch.isChecked = true

                        TracksManager.uploadNewTracks()

                        requestMetaDataDisclosure()
                    },
                    {
                        requestMetaDataDisclosure()
                    }
                )
            }
        }
    }

    private fun updateUserStatus(status: String) {
        UserSettingsManager.status = status

        KeysManager.uploadNewKeys(true)

        refreshStatus()
    }

    private fun refreshStatus() {
        if (UserSettingsManager.sick()) {
            currentStatusTextView.text =
                getString(R.string.current_status, getString(R.string.status_symptoms))

            changeStatusButton.setText(R.string.whats_next)
            changeStatusButton.setBackgroundResource(R.drawable.bg_green_button)

            shareTrackSwitch.isEnabled = true
            shareTrackSwitch.isChecked = UserSettingsManager.uploadTrack

            shareMetaSwitch.isEnabled = true
            shareMetaSwitch.isChecked = UserSettingsManager.discloseMetaData
        } else {
            currentStatusTextView.text =
                getString(R.string.current_status, getString(R.string.status_normal))

            changeStatusButton.setText(R.string.i_got_symptoms)
            changeStatusButton.setBackgroundResource(R.drawable.bg_red_button)
        }
    }

    private fun requestMetaDataDisclosure() {
        choose(
            R.string.share_meta_data_confirmation,
            {
                UserSettingsManager.discloseMetaData = true
                shareMetaSwitch.isChecked = true

                KeysManager.uploadNewKeys(true)
            },
            {
                KeysManager.uploadNewKeys(true)
            }
        )
    }

}
