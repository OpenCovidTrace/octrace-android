package org.opencovidtrace.octrace.storage

import org.opencovidtrace.octrace.utils.CryptoUtil
import java.util.*

object DataManager {

    const val maxDays = 14

    private fun expirationDate(): Date =
        Calendar.getInstance().apply { add(Calendar.DATE, -maxDays) }.time

    fun expirationTimestamp() = expirationDate().time

    fun expirationDay() = CryptoUtil.getDayNumber(expirationDate())

}
