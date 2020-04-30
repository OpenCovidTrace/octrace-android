package org.opencovidtrace.octrace.ui.map.qrcode

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.opencovidtrace.octrace.di.api.ContactsApiClientProvider
import org.opencovidtrace.octrace.storage.EncryptionKeysManager
import org.opencovidtrace.octrace.utils.CryptoUtil
import org.opencovidtrace.octrace.utils.CryptoUtil.base64EncodedString
import java.util.*

class QrCodeViewModel : ViewModel() {

    fun generateBitmap(token: String, qrCodeCallback: (Bitmap?) -> Unit) {
        val rollingId = CryptoUtil.getRollingId().base64EncodedString()
        val tst = System.currentTimeMillis()
        val key = EncryptionKeysManager.generateKey(tst).base64EncodedString()
        val url: String =
            Uri.parse(ContactsApiClientProvider.CONTACTS_ENDPOINT + "app/contact")
                .buildUpon()
                .appendQueryParameter("p", "android")
                .appendQueryParameter("d", token)
                .appendQueryParameter("i", rollingId)
                .appendQueryParameter("k", key)
                .appendQueryParameter("t", String.format("%d", tst))
                .build().toString()
        qrCodeCallback(generateBitmap(url))
    }

    private fun generateBitmap(str: String?): Bitmap? {
        if (str.isNullOrEmpty())
            return null
        val size = 300
        val result: BitMatrix
        try {
            val hints = Hashtable<EncodeHintType, String>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            result = MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, size, size, hints)
        } catch (iae: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) Color.BLACK else Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, size, 0, 0, w, h)
        return bitmap
    }
}