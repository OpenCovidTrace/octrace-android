package org.opencovidtrace.octrace.ui.map.qrcode

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.andrefrsousa.superbottomsheet.SuperBottomSheetFragment
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.fragment_qr_code.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.opencovidtrace.octrace.R
import org.opencovidtrace.octrace.data.MakeContactEvent
import org.opencovidtrace.octrace.ext.ui.showError


class QrCodeFragment : SuperBottomSheetFragment() {

    private lateinit var qrCodeViewModel: QrCodeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        qrCodeViewModel =
            ViewModelProvider(this).get(QrCodeViewModel::class.java)
        return inflater.inflate(R.layout.fragment_qr_code, container, false)
    }

    override fun getCornerRadius() = resources.getDimension(R.dimen.sheet_rounded_corner)

    override fun animateCornerRadius() = false

    override fun isSheetAlwaysExpanded(): Boolean = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        FirebaseInstanceId.getInstance().instanceId
            .addOnSuccessListener { result ->
                qrCodeViewModel.generateBitmap(result.token, ::onQrGenerate)
            }
            .addOnCanceledListener { showError(R.string.failed_get_token) }
        closeImageButton.setOnClickListener { dismiss() }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMakeContactEvent(event: MakeContactEvent) {
        dismiss()
    }

    private fun onQrGenerate(bitmap: Bitmap?) = if (bitmap != null)
        qrCodeImageView.setImageBitmap(bitmap)
    else
        showError(R.string.failed_generate_qr)

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)

        super.onDestroyView()
    }
}