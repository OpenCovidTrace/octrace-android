package org.opencovidtrace.octrace.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_status.*
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.ext.ui.confirm
import org.opencovidtrace.octrace.ext.ui.showInfo
import org.opencovidtrace.octrace.storage.KeysManager
import org.opencovidtrace.octrace.storage.TracksManager
import org.opencovidtrace.octrace.storage.UserStatusManager

class StatusFragment : Fragment() {

    private lateinit var statusViewModel: StatusViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        statusViewModel =
            ViewModelProvider(this).get(StatusViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_status, container, false)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        changeStatusButton.setOnClickListener { changeStatus() }
        refreshStatus()
    }

    private fun changeStatus() {
        if (UserStatusManager.sick()) {
            showInfo(R.string.tatus_sick_info)
        } else {
            confirm(R.string.change_status_info_1) {
                updateUserStatus(UserStatusManager.symptoms)
            }
        }
    }

    private fun updateUserStatus(status: String) {
        UserStatusManager.setStatus(status)

        TracksManager.uploadNewTracks()
        KeysManager.uploadNewKeys()

        refreshStatus()
    }

    private fun refreshStatus() {
        if (UserStatusManager.sick()) {
            currentStatusTextView.text =
                getString(R.string.current_status, getString(R.string.status_symptoms))

            changeStatusButton.setText(R.string.whats_next)
            changeStatusButton.setBackgroundResource(R.drawable.bg_rounded_green_button)
        } else {
            currentStatusTextView.text =
                getString(R.string.current_status, getString(R.string.status_healthy))

            changeStatusButton.setText(R.string.i_got_symptoms)
            changeStatusButton.setBackgroundResource(R.drawable.bg_rounded_red_button)
        }
    }
}